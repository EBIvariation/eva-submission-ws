package uk.ac.ebi.eva.submission.model;

public class SubmittedVcfDto {
    private String analysisAlias;
    private String submittedFile;
    private String brokeredFile;
    private String csiFile;
    private String brokeredFileMd5;
    private String csiFileMd5;

    public String getSubmittedFile() {
        return submittedFile;
    }

    public String getAnalysisAlias() {
        return analysisAlias;
    }

    public void setAnalysisAlias(String analysisAlias) {
        this.analysisAlias = analysisAlias;
    }

    public void setSubmittedFile(String submittedFile) {
        this.submittedFile = submittedFile;
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
