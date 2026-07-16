package uk.ac.ebi.eva.submission.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface SubmissionSummaryProjection {
    String getSubmissionId();
    LocalDateTime getUploadedTime();
    String getAccountId();
    String getEloadSource();
    Integer getEloadId();
    String getProcessingStep();
    String getProcessingStatus();
    String getProjectTitle();
}
