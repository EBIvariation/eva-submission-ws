package uk.ac.ebi.eva.submission.model;

import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "submission")
public class Submission {

    public Submission() {

    }

    public Submission(String submissionId) {
        this.submissionId = submissionId;
    }

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;

    @NonNull
    @Column(nullable = false)
    private String status;

    @NonNull
    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false)
    private LocalDateTime initiationTime;

    @Column
    private LocalDateTime uploadedTime;

    @Column
    private LocalDateTime completionTime;

    @Column
    private String uploadUrl;

    public String getSubmissionId() {
        return submissionId;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public LocalDateTime getInitiationTime() {
        return initiationTime;
    }

    public void setInitiationTime(LocalDateTime initiationTime) {
        this.initiationTime = initiationTime;
    }

    public LocalDateTime getUploadedTime() {
        return uploadedTime;
    }

    public void setUploadedTime(LocalDateTime uploadedTime) {
        this.uploadedTime = uploadedTime;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Submission)) return false;
        Submission that = (Submission) o;
        return getSubmissionId().equals(that.getSubmissionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubmissionId());
    }

    @Override
    public String toString() {
        return "Submission{" +
                "submissionId='" + submissionId + '\'' +
                ", status=" + status +
                ", initiationTime=" + initiationTime +
                ", uploadedTime=" + uploadedTime +
                ", completionTime=" + completionTime +
                '}';
    }
}
