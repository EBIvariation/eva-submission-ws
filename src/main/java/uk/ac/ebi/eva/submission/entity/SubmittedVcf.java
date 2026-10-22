package uk.ac.ebi.eva.submission.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@IdClass(SubmittedVcf.SubmittedVcfId.class)
@Table(schema = "eva_submissions", name = "submitted_vcf")
public class SubmittedVcf {

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @Id
    @Column(name = "analysis_alias")
    private String analysisAlias;

    @Id
    @Column(name = "submitted_file")
    private String submittedFile;

    @Column(name = "brokered_file")
    private String brokeredFile;

    @Column(name = "csi_file")
    private String csiFile;

    @Column(name = "brokered_file_md5")
    private String brokeredFileMd5;

    @Column(name = "csi_file_md5")
    private String csiFileMd5;

    public static final class SubmittedVcfId {

        private String submissionId;

        private String analysisAlias;

        private String submittedFile;

        public SubmittedVcfId() {
        }

        public SubmittedVcfId(String submissionId, String analysisAlias, String submittedFile) {
            this.submissionId = submissionId;
            this.analysisAlias = analysisAlias;
            this.submittedFile = submittedFile;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            SubmittedVcfId that = (SubmittedVcfId) object;
            return Objects.equals(submissionId, that.submissionId)
                    && Objects.equals(analysisAlias, that.analysisAlias)
                    && Objects.equals(submittedFile, that.submittedFile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(submissionId, analysisAlias, submittedFile);
        }
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getAnalysisAlias() {
        return analysisAlias;
    }

    public String getSubmittedFile() {
        return submittedFile;
    }

    public String getBrokeredFile() {
        return brokeredFile;
    }

    public void setBrokeredFile(String brokeredFile) {
        this.brokeredFile = brokeredFile;
    }

    public String getCsiFile() {
        return csiFile;
    }

    public void setCsiFile(String csiFile) {
        this.csiFile = csiFile;
    }

    public String getBrokeredFileMd5() {
        return brokeredFileMd5;
    }

    public void setBrokeredFileMd5(String brokeredFileMd5) {
        this.brokeredFileMd5 = brokeredFileMd5;
    }

    public String getCsiFileMd5() {
        return csiFileMd5;
    }

    public void setCsiFileMd5(String csiFileMd5) {
        this.csiFileMd5 = csiFileMd5;
    }
}
