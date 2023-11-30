package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.model.Submission;

public interface SubmissionRepository extends CrudRepository<Submission, String> {
    Submission findBySubmissionId(String submissionId);
}
