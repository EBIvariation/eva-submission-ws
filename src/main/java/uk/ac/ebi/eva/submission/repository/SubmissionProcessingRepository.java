package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;

import java.util.List;

public interface SubmissionProcessingRepository extends CrudRepository<SubmissionProcessing, String> {

    SubmissionProcessing findBySubmissionId(String submissionId);

    List<SubmissionProcessing> findByStepAndStatus(String step, String status);
}
