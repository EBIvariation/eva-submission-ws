package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmittedSample;

public interface SubmittedSampleRepository extends CrudRepository<SubmittedSample, Long> {
}
