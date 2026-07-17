package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmissionTrackingDetails;

public interface SubmissionTrackingDetailsRepository extends CrudRepository<SubmissionTrackingDetails, String> {
    SubmissionTrackingDetails findBySubmissionId(String submissionId);
}
