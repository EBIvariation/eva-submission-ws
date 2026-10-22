package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@IdClass(SubmittedSample.SubmittedSampleId.class)
@Table(schema = "eva_submissions", name = "submitted_sample")
public class SubmittedSample {

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @Id
    @Column(name = "analysis_alias")
    private String analysisAlias;

    @Id
    @Column(name = "sample_name")
    private String sampleName;

    @Column(name = "sample_accession")
    private String sampleAccession;

    public static final class SubmittedSampleId {

        private String submissionId;

        private String sampleName;

        public SubmittedSampleId() {
        }

        public SubmittedSampleId(String submissionId, String sampleName) {
            this.submissionId = submissionId;
            this.sampleName = sampleName;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            SubmittedSampleId that = (SubmittedSampleId) object;
            return Objects.equals(submissionId, that.submissionId)
                    && Objects.equals(sampleName, that.sampleName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(submissionId, sampleName);
        }
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getSampleAccession() {
        return sampleAccession;
    }

    public void setSampleAccession(String sampleAccession) {
        this.sampleAccession = sampleAccession;
    }
}
