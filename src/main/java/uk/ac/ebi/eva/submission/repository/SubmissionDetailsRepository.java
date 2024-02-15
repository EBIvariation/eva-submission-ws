package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;

public interface SubmissionDetailsRepository extends CrudRepository<SubmissionDetails, String> {
}
