package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import uk.ac.ebi.eva.submission.entity.SubmittedSample;

public interface SubmittedSampleRepository extends CrudRepository<SubmittedSample, SubmittedSample.SubmittedSampleId> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM eva_submissions.submitted_sample s WHERE s.submission_id = :submissionId " +
            "AND NOT EXISTS (SELECT 1 FROM eva_submissions.submitted_analysis_submitted_sample sass " +
            "WHERE sass.sample_submission_id = s.submission_id AND sass.sample_name = s.sample_name)",
            nativeQuery = true)
    int deleteOrphanedSamples(@Param("submissionId") String submissionId);

}
