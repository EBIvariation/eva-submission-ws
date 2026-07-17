package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import uk.ac.ebi.eva.submission.entity.Submission;

import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionRepository extends CrudRepository<Submission, String> {
    Submission findBySubmissionId(String submissionId);

    List<Submission> findByStatus(String status);

    @Query(value =
            "SELECT s.submission_id AS submissionId, s.uploaded_time AS uploadedTime, sa.id AS accountId, " +
            "se.source AS eloadSource, se.eload AS eloadId, " +
            "sp.step AS processingStep, sp.status AS processingStatus, sd.project_title AS projectTitle, " +
            "st.project_accession AS projectAccession, sta.analysis_accessions AS analysisAccessions, " +
            "st.release_date AS releaseDate, st.rt_link AS rtLink " +
            "FROM eva_submissions.submission s " +
            "JOIN eva_submissions.submission_account sa ON sa.id = s.submission_account_id " +
            "LEFT JOIN eva_submissions.submission_eload se ON se.submission_id = s.submission_id " +
            "LEFT JOIN eva_submissions.submission_processing_status sp ON sp.submission_id = s.submission_id " +
            "LEFT JOIN eva_submissions.submission_details sd ON sd.submission_id = s.submission_id " +
            "LEFT JOIN eva_submissions.submission_tracking_details st ON st.submission_id = s.submission_id " +
            "LEFT JOIN (" +
            "    SELECT submission_id, string_agg(analysis_accessions, ',') AS analysis_accessions" +
            "    FROM eva_submissions.submission_tracking_details_analysis_accessions" +
            "    GROUP BY submission_id" +
            ") sta ON sta.submission_id = s.submission_id " +
            "WHERE (CAST(:submissionAccount AS text) IS NULL OR sa.id = :submissionAccount) " +
            "AND (CAST(:uploadedAfter AS timestamp) IS NULL OR s.uploaded_time >= CAST(:uploadedAfter AS timestamp)) " +
            "AND (CAST(:source AS text) IS NULL OR se.source = :source) " +
            "AND (CAST(:processingStep AS text) IS NULL OR sp.step = :processingStep) " +
            "AND (CAST(:processingStatus AS text) IS NULL OR sp.status = :processingStatus) " +
            "AND (CAST(:submissionId AS text) IS NULL OR s.submission_id = :submissionId) " +
            "AND (CAST(:eloadId AS integer) IS NULL OR se.eload = :eloadId)",
            countQuery =
            "SELECT COUNT(*) " +
            "FROM eva_submissions.submission s " +
            "JOIN eva_submissions.submission_account sa ON sa.id = s.submission_account_id " +
            "LEFT JOIN eva_submissions.submission_eload se ON se.submission_id = s.submission_id " +
            "LEFT JOIN eva_submissions.submission_processing_status sp ON sp.submission_id = s.submission_id " +
            "WHERE (CAST(:submissionAccount AS text) IS NULL OR sa.id = :submissionAccount) " +
            "AND (CAST(:uploadedAfter AS timestamp) IS NULL OR s.uploaded_time >= CAST(:uploadedAfter AS timestamp)) " +
            "AND (CAST(:source AS text) IS NULL OR se.source = :source) " +
            "AND (CAST(:processingStep AS text) IS NULL OR sp.step = :processingStep) " +
            "AND (CAST(:processingStatus AS text) IS NULL OR sp.status = :processingStatus) " +
            "AND (CAST(:submissionId AS text) IS NULL OR s.submission_id = :submissionId) " +
            "AND (CAST(:eloadId AS integer) IS NULL OR se.eload = :eloadId)",
            nativeQuery = true)
    Page<SubmissionSummaryProjection> findSubmissionSummaries(
            @Param("submissionAccount") String submissionAccount,
            @Param("uploadedAfter")     LocalDateTime uploadedAfter,
            @Param("source")            String source,
            @Param("processingStep")    String processingStep,
            @Param("processingStatus")  String processingStatus,
            @Param("submissionId")      String submissionId,
            @Param("eloadId")           Integer eloadId,
            Pageable pageable);
}
