package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;
import uk.ac.ebi.eva.submission.exception.MetadataFileInfoMismatchException;
import uk.ac.ebi.eva.submission.exception.RequiredFieldsMissingException;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStatus;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStep;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionAccountRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionDetailsRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionProcessingRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.util.EmailNotificationHelper;
import uk.ac.ebi.eva.submission.util.MailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SubmissionService {
    private static final String PROJECT = "project";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String METADATA_FILES_TAG = "files";
    private static final String METADATA_FILE_NAME = "fileName";
    private static final String METADATA_FILE_SIZE = "fileSize";
    private static final String GLOBUS_FILES_TAG = "DATA";
    private static final String GLOBUS_FILE_NAME = "name";
    private static final String GLOBUS_FILE_SIZE = "size";

    private final SubmissionRepository submissionRepository;

    private final SubmissionAccountRepository submissionAccountRepository;

    private final SubmissionDetailsRepository submissionDetailsRepository;

    private final SubmissionProcessingRepository submissionProcessingRepository;

    private final GlobusDirectoryProvisioner globusDirectoryProvisioner;

    private final MailSender mailSender;

    @Value("${globus.uploadHttpDomain}")
    private String uploadHttpDomain;

    private EmailNotificationHelper emailHelper;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionAccountRepository submissionAccountRepository,
                             SubmissionDetailsRepository submissionDetailsRepository,
                             SubmissionProcessingRepository submissionProcessingRepository,
                             GlobusDirectoryProvisioner globusDirectoryProvisioner,
                             MailSender mailSender, EmailNotificationHelper emailHelper) {
        this.submissionRepository = submissionRepository;
        this.submissionAccountRepository = submissionAccountRepository;
        this.submissionDetailsRepository = submissionDetailsRepository;
        this.submissionProcessingRepository = submissionProcessingRepository;
        this.globusDirectoryProvisioner = globusDirectoryProvisioner;
        this.mailSender = mailSender;
        this.emailHelper = emailHelper;
    }

    public Submission initiateSubmission(SubmissionAccount submissionAccount) {
        String submissionId = UUID.randomUUID().toString();
        String directoryToCreate = String.format("%s/%s", submissionAccount.getId(), submissionId);
        globusDirectoryProvisioner.createSubmissionDirectory(directoryToCreate);

        Optional<SubmissionAccount> optSubmissionAccount = submissionAccountRepository.findById(submissionAccount.getId());
        // if the user account is not present or if its primary email has changed, save/update the user account
        if (!optSubmissionAccount.isPresent() || !optSubmissionAccount.get().getPrimaryEmail().equals(submissionAccount.getPrimaryEmail())) {
            submissionAccountRepository.save(submissionAccount);
        }

        Submission submission = new Submission(submissionId);
        submission.setSubmissionAccount(submissionAccount);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submission.setUploadUrl(uploadHttpDomain + "/" + directoryToCreate);

        return submissionRepository.save(submission);
    }

    public void checkMetadataFileInfoMatchesWithUploadedFiles(SubmissionAccount submissionAccount, String submissionId, JsonNode metadataJson) {
        String directoryToList = String.format("%s/%s", submissionAccount.getId(), submissionId);
        String uploadedFilesInfo = globusDirectoryProvisioner.listSubmittedFiles(directoryToList);
        if (uploadedFilesInfo.isEmpty()) {
            throw new MetadataFileInfoMismatchException("Failed to retrieve any file info from submission directory.");
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode globusFileInfoJson = (ObjectNode) mapper.readTree(uploadedFilesInfo);
                Map<String, Long> globusFileInfo = new HashMap<>();
                if (globusFileInfoJson.get(GLOBUS_FILES_TAG) != null) {
                    globusFileInfo = StreamSupport.stream(globusFileInfoJson.get(GLOBUS_FILES_TAG).spliterator(), false)
                            .filter(dataNode -> dataNode.get(GLOBUS_FILE_NAME).asText().endsWith(".vcf") || dataNode.get(GLOBUS_FILE_NAME).asText().endsWith(".vcf.gz"))
                            .collect(Collectors.toMap(
                                    dataNode -> dataNode.get(GLOBUS_FILE_NAME).asText(),
                                    dataNode -> dataNode.get(GLOBUS_FILE_SIZE).asLong()
                            ));
                }

                Map<String, Long> metadataFileInfo = StreamSupport.stream(metadataJson.get(METADATA_FILES_TAG).spliterator(), false)
                        .collect(Collectors.toMap(
                                dataNode -> dataNode.get(METADATA_FILE_NAME).asText(),
                                dataNode -> dataNode.get(METADATA_FILE_SIZE).asLong()
                        ));

                List<String> missingFileList = new ArrayList<>();
                String fileSizeMismatchInfo = "";

                for (Map.Entry<String, Long> fileEntry : metadataFileInfo.entrySet()) {
                    String fileName = fileEntry.getKey();
                    Long metadataFileSize = fileEntry.getValue();
                    if (globusFileInfo.containsKey(fileName)) {
                        Long fileSizeInGlobus = globusFileInfo.get(fileName);
                        if (!metadataFileSize.equals(fileSizeInGlobus)) {
                            fileSizeMismatchInfo += fileName + ": metadata json file size (" + metadataFileSize + ") is not equal to uploaded file size (" + fileSizeInGlobus + ")\n";
                        }
                    } else {
                        missingFileList.add(fileName);
                    }
                }

                if (!missingFileList.isEmpty() || !fileSizeMismatchInfo.isEmpty()) {
                    String missingFileMsg = missingFileList.isEmpty() ? "" : "There are some files mentioned in metadata json but not uploaded. Files : " + String.join(", ", missingFileList) + "\n";
                    String fileSizeMismatchMsg = fileSizeMismatchInfo.isEmpty() ? "" : "There are some files mentioned in metadata json whose size does not match with the files uploaded.\n" + fileSizeMismatchInfo;
                    throw new MetadataFileInfoMismatchException(missingFileMsg + fileSizeMismatchMsg);
                }
            } catch (JsonProcessingException ex) {
                throw new MetadataFileInfoMismatchException("Error parsing fileInfo from Submission Directory");
            }
        }
    }

    public Submission uploadMetadataJsonAndMarkUploaded(String submissionId, JsonNode metadataJson) {
        SubmissionDetails submissionDetails = new SubmissionDetails(submissionId);
        try {
            JsonNode project = metadataJson.get(PROJECT);
            String projectTitle = project.get(TITLE).asText();
            String projectDescription = project.get(DESCRIPTION).asText();

            submissionDetails.setProjectTitle(projectTitle);
            submissionDetails.setProjectDescription(projectDescription);
        } catch (Exception e) {
            throw new RequiredFieldsMissingException("Required fields project title and project description " +
                    "could not be found in metadata json");
        }

        submissionDetails.setMetadataJson(metadataJson);
        submissionDetailsRepository.save(submissionDetails);

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        submission.setStatus(SubmissionStatus.UPLOADED.toString());
        submission.setUploadedTime(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    public String getSubmissionStatus(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        if (submission == null) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        return submission.getStatus();
    }

    public Submission markSubmissionStatus(String submissionId, SubmissionStatus status) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        if (submission == null) {
            throw new SubmissionDoesNotExistException(submissionId);
        }
        submission.setStatus(status.toString());
        if (status == SubmissionStatus.COMPLETED) {
            submission.setCompletionTime(LocalDateTime.now());
        }

        return submissionRepository.save(submission);
    }

    public boolean checkUserHasAccessToSubmission(SubmissionAccount account, String submissionId) {
        Optional<Submission> optSubmission = submissionRepository.findById(submissionId);
        if (optSubmission.isPresent()) {
            SubmissionAccount submissionAccount = optSubmission.get().getSubmissionAccount();
            return submissionAccount.getId().equals(account.getId());
        } else {
            throw new SubmissionDoesNotExistException(submissionId);
        }
    }

    public void sendMailNotificationForStatusUpdate(SubmissionAccount submissionAccount, String submissionId,
                                                    SubmissionStatus submissionStatus, boolean success) {
        String sendTo = submissionAccount.getPrimaryEmail();
        String subject = emailHelper.getSubjectForSubmissionStatusUpdate(submissionStatus, success);
        String body = emailHelper.getTextForSubmissionStatusUpdate(submissionAccount, submissionId, submissionStatus, success);
        mailSender.sendEmail(sendTo, subject, body);
    }

    public List<Submission> getSubmissionsByStatus(SubmissionStatus status) {
        return submissionRepository.findByStatus(status.toString());
    }

    public List<SubmissionProcessing> getSubmissionsByProcessingStepAndStatus(SubmissionProcessingStep step,
                                                                              SubmissionProcessingStatus status) {
        return submissionProcessingRepository.findByStepAndStatus(step.toString(), status.toString());

    }

    public SubmissionProcessing markSubmissionProcessStepAndStatus(String submissionId,
                                                                   SubmissionProcessingStep step,
                                                                   SubmissionProcessingStatus status) {
        Optional<Submission> submission = submissionRepository.findById(submissionId);
        if (!submission.isPresent()) {
            throw new SubmissionDoesNotExistException(submissionId);
        }

        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        if (submissionProc == null) {
            submissionProc = new SubmissionProcessing(submissionId);
        }

        submissionProc.setStep(step.toString());
        submissionProc.setStatus(status.toString());
        return submissionProcessingRepository.save(submissionProc);
    }


}
