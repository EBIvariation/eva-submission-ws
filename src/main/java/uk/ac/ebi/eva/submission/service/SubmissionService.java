package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.model.SubmissionUser;
import uk.ac.ebi.eva.submission.repository.SubmissionDetailsRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionUserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    private final SubmissionUserRepository userRepository;

    private final SubmissionDetailsRepository submissionDetailsRepository;

    private final GlobusDirectoryProvisioner globusDirectoryProvisioner;

    @Value("${globus.uploadHttpDomain}")
    private String uploadHttpDomain;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionUserRepository submissionUserRepository,
                             SubmissionDetailsRepository submissionDetailsRepository,
                             GlobusDirectoryProvisioner globusDirectoryProvisioner) {
        this.submissionRepository = submissionRepository;
        this.userRepository = submissionUserRepository;
        this.submissionDetailsRepository = submissionDetailsRepository;
        this.globusDirectoryProvisioner = globusDirectoryProvisioner;
    }

    public Submission initiateSubmission(SubmissionUser submissionUser) {
        String submissionId = UUID.randomUUID().toString();
        String directoryToCreate = String.format("%s/%s", submissionUser.getId(), submissionId);
        globusDirectoryProvisioner.createSubmissionDirectory(directoryToCreate);

        Optional<SubmissionUser> optSubmissionUser = userRepository.findById(submissionUser.getId());
        if (!optSubmissionUser.isPresent()) {
            userRepository.save(submissionUser);
        }

        Submission submission = new Submission(submissionId);
        submission.setUser(submissionUser);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submission.setUploadUrl(uploadHttpDomain + "/" + directoryToCreate);

        return submissionRepository.save(submission);
    }

    public Submission markSubmissionUploaded(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        submission.setStatus(SubmissionStatus.UPLOADED.toString());
        submission.setUploadedTime(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    public String getSubmissionStatus(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
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

    public boolean checkUserHasAccessToSubmission(SubmissionUser user, String submissionId){
        Optional<Submission> optSubmission = submissionRepository.findById(submissionId);
        if(optSubmission.isPresent()){
            SubmissionUser submissionUser = optSubmission.get().getUser();
            return submissionUser.getId() == user.getId();
        }else{
            throw new RuntimeException("Given submission with id " + submissionId + "does not exist");
        }
    }
}
