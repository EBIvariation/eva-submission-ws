package uk.ac.ebi.eva.submission.model;

import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "submission_status")
public class SubmissionStatus {

    public SubmissionStatus() {

    }

    public SubmissionStatus(String submissionId) {
        this.submissionId = submissionId;
    }

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;

    @NonNull
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean completed;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime dirCreatedTime;


    public String getSubmissionId() {
        return submissionId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getDirCreatedTime() {
        return dirCreatedTime;
    }

    public void setSubmissionId(@NonNull String submissionId) {
        this.submissionId = submissionId;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionStatus)) return false;
        SubmissionStatus that = (SubmissionStatus) o;
        return getSubmissionId().equals(that.getSubmissionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubmissionId());
    }

    @Override
    public String toString() {
        return "SubmissionStatus{" +
                "submissionId='" + submissionId + '\'' +
                ", completed=" + completed +
                ", dirCreatedTime=" + dirCreatedTime +
                '}';
    }
}
