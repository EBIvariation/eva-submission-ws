package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionService {
    private static final String PROJECT = "project";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

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
            throw new SubmissionDoesNotExistException("Submission with Id " + submissionId + " does not exist");
        }

        return submission.getStatus();
    }

    public Submission markSubmissionStatus(String submissionId, SubmissionStatus status) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
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
            throw new SubmissionDoesNotExistException("Given submission with id " + submissionId + " does not exist");
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
        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        submissionProc.setStep(step.toString());
        submissionProc.setStatus(status.toString());
        return submissionProcessingRepository.save(submissionProc);
    }


}
