package uk.ac.ebi.eva.submission.model;

public class SubmittedSampleDto {
    private String analysisAlias;
    private String sampleName;
    private String sampleAccession;

    public String getSampleName() {
        return sampleName;
    }

    public String getAnalysisAlias() {
        return analysisAlias;
    }

    public void setAnalysisAlias(String analysisAlias) {
        this.analysisAlias = analysisAlias;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleAccession() {
        return sampleAccession;
    }

    public void setSampleAccession(String sampleAccession) {
        this.sampleAccession = sampleAccession;
    }
}
