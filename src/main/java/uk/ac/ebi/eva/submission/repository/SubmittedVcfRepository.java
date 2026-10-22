package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmittedVcf;

public interface SubmittedVcfRepository extends CrudRepository<SubmittedVcf, Long> {
}
