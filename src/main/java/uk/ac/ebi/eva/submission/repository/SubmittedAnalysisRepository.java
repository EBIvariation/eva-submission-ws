package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmittedAnalysis;

import java.util.List;

public interface SubmittedAnalysisRepository extends CrudRepository<SubmittedAnalysis, SubmittedAnalysis.SubmittedAnalysisId> {

    List<SubmittedAnalysis> findAllBySubmissionId(String submissionId);

}
