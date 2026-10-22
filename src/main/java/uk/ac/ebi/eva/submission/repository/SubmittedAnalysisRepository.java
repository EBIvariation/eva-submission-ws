package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmittedAnalysis;

public interface SubmittedAnalysisRepository extends CrudRepository<SubmittedAnalysis, Long> {
}
