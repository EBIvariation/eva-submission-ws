package uk.ac.ebi.eva.submission.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubmissionSummaryDto {

    private String submissionId;
    private LocalDateTime uploadedTime;
    private String accountId;
    private String eloadSource;
    private Integer eloadId;
    private String processingStep;
    private String processingStatus;
    private String projectTitle;
    private LocalDate releaseDate;

    public SubmissionSummaryDto(String submissionId, LocalDateTime uploadedTime, String accountId,
                                String eloadSource, Integer eloadId,
                                String processingStep, String processingStatus,
                                String projectTitle, LocalDate releaseDate) {
        this.submissionId = submissionId;
        this.uploadedTime = uploadedTime;
        this.accountId = accountId;
        this.eloadSource = eloadSource;
        this.eloadId = eloadId;
        this.processingStep = processingStep;
        this.processingStatus = processingStatus;
        this.projectTitle = projectTitle;
        this.releaseDate = releaseDate;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEloadSource() {
        return eloadSource;
    }

    public Integer getEloadId() {
        return eloadId;
    }

    public String getProcessingStep() {
        return processingStep;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }
}
