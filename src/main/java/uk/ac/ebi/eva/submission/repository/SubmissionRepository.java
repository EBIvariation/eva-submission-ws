package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.Submission;

public interface SubmissionRepository extends CrudRepository<Submission, String> {
    Submission findBySubmissionId(String submissionId);
}
