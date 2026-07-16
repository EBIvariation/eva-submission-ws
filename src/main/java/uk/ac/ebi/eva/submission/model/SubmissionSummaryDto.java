package uk.ac.ebi.eva.submission.model;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SubmissionSummaryDto {

    private String submissionId;
    private String submissionStatus;
    private LocalDateTime uploadedTime;
    private String accountId;
    private String eloadSource;
    private Integer eloadId;
    private String processingStep;
    private String processingStatus;
    private String projectTitle;
    private LocalDate releaseDate;
    private String projectAccession;
    private List<String> analysisAccessions;
    private URI rtLink;

    public SubmissionSummaryDto(String submissionId, String submissionStatus, LocalDateTime uploadedTime, String accountId,
                                String eloadSource, Integer eloadId,
                                String processingStep, String processingStatus,
                                String projectTitle, LocalDate releaseDate,
                                String projectAccession, List<String> analysisAccessions, URI rtLink) {
        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
        this.uploadedTime = uploadedTime;
        this.accountId = accountId;
        this.eloadSource = eloadSource;
        this.eloadId = eloadId;
        this.processingStep = processingStep;
        this.processingStatus = processingStatus;
        this.projectTitle = projectTitle;
        this.releaseDate = releaseDate;
        this.projectAccession = projectAccession;
        this.analysisAccessions = analysisAccessions;
        this.rtLink = rtLink;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
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

    public String getProjectAccession() {
        return projectAccession;
    }

    public List<String> getAnalysisAccessions() {
        return analysisAccessions;
    }

    public URI getRtLink() {
        return rtLink;
    }
}
