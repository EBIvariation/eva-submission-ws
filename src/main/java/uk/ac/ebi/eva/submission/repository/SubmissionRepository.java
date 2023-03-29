package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;

public interface SubmissionRepository extends CrudRepository<SubmissionStatus, Long> {
    SubmissionStatus findBySubmissionId(String submissionId);
}
