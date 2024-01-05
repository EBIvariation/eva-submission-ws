package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.model.SubmissionDetails;

public interface SubmissionDetailsRepository extends CrudRepository<SubmissionDetails, String> {
}
