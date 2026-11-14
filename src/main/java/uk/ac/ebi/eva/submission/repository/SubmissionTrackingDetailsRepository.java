package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import jakarta.transaction.Transactional;
import uk.ac.ebi.eva.submission.entity.SubmissionTrackingDetails;

public interface SubmissionTrackingDetailsRepository extends CrudRepository<SubmissionTrackingDetails, String> {
    SubmissionTrackingDetails findBySubmissionId(String submissionId);

    @Transactional
    default void deleteBySubmissionId(String submissionId) {
        SubmissionTrackingDetails details = findBySubmissionId(submissionId);
        if (details != null) {
            delete(details);
        }
    }
}
