package uk.ac.ebi.eva.submission.service;

import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import java.util.UUID;

@Service
public class SubmissionService {

    private SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public SubmissionStatus createFtpDirectory() {
        String submissionId = UUID.randomUUID().toString();

        SubmissionStatus submissionStatus = new SubmissionStatus(submissionId);
        return submissionRepository.save(submissionStatus);
    }

    public SubmissionStatus markSubmissionComplete(String submissionId) {
        SubmissionStatus submissionStatus = submissionRepository.findBySubmissionId(submissionId);
        submissionStatus.setCompleted(true);

        return submissionRepository.save(submissionStatus);
    }
}
