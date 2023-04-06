package uk.ac.ebi.eva.submission.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SubmissionService {

    private SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public Submission initiateSubmission() {
        String submissionId = UUID.randomUUID().toString();

        Submission submission = new Submission(submissionId);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());

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
