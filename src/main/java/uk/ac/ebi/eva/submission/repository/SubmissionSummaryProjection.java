package uk.ac.ebi.eva.submission.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionSummaryProjection {
    String getSubmissionId();
    LocalDateTime getUploadedTime();
    String getAccountId();
    String getEloadSource();
    Integer getEloadId();
    String getProcessingStep();
    String getProcessingStatus();
    String getProjectTitle();
    LocalDate getReleaseDate();
    String getProjectAccession();
    List<String> getAnalysisAccessions();
}
