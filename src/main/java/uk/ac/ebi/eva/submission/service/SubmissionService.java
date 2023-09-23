package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GlobusDirectoryProvisioner globusDirectoryProvisioner;

    @Value("${globus.uploadHttpDomain}")
    private String uploadHttpDomain;

    public SubmissionService(SubmissionRepository submissionRepository,
                             GlobusDirectoryProvisioner globusDirectoryProvisioner) {
        this.submissionRepository = submissionRepository;
        this.globusDirectoryProvisioner = globusDirectoryProvisioner;
    }

    public Submission initiateSubmission(String userId) {
        String submissionId = UUID.randomUUID().toString();
        String directoryToCreate = String.format("%s/%s", userId, submissionId);
        globusDirectoryProvisioner.createSubmissionDirectory(directoryToCreate);

        Submission submission = new Submission(submissionId);
        submission.setUserId(userId);
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
}
