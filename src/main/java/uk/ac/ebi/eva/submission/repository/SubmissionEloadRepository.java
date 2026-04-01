package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmissionEload;

import java.util.List;

public interface SubmissionEloadRepository extends CrudRepository<SubmissionEload, String> {
    SubmissionEload findBySubmissionId(String submissionId);

    SubmissionEload findByEload(Integer eload);

    List<SubmissionEload> findBySubmissionIdOrEload(String submissionId, Integer eload);
}
