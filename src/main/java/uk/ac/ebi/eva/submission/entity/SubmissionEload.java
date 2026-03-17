package uk.ac.ebi.eva.submission.entity;

import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;


@Entity
@Table(schema = "eva_submissions", name = "submission_eload")
public class SubmissionEload {

    public SubmissionEload() {

    }

    public SubmissionEload(String submissionId, Integer eload) {
        this.submissionId = submissionId;
        this.eload = eload;
    }

    @Id
    @NonNull
    @Column(nullable = false, name = "submission_id")
    private String submissionId;

    @NonNull
    @Column(nullable = false, unique = true)
    private Integer eload;

    public String getSubmissionId() {
        return submissionId;
    }

    @NonNull
    public Integer getEload() {
        return eload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionEload)) return false;
        SubmissionEload that = (SubmissionEload) o;
        return getSubmissionId().equals(that.getSubmissionId()) && getEload().equals(that.getEload());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubmissionId() + getEload());
    }

    @Override
    public String toString() {
        return "SubmissionEload{" +
                "submissionId='" + submissionId + '\'' +
                ", eload=" + eload +
                '}';
    }
}