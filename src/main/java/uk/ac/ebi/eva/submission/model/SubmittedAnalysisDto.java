package uk.ac.ebi.eva.submission.model;

import java.util.List;

public class SubmittedAnalysisDto {
    private String analysisAlias;
    private String analysisAccession;
    private String aggregationType;
    private String assemblyAccession;
    private List<SubmittedSampleDto> samples;
    private List<SubmittedVcfDto> vcfs;

    public String getAnalysisAlias() { return analysisAlias; }
    public void setAnalysisAlias(String analysisAlias) { this.analysisAlias = analysisAlias; }
    public String getAnalysisAccession() { return analysisAccession; }
    public void setAnalysisAccession(String analysisAccession) { this.analysisAccession = analysisAccession; }
    public String getAggregationType() { return aggregationType; }
    public void setAggregationType(String aggregationType) { this.aggregationType = aggregationType; }
    public String getAssemblyAccession() { return assemblyAccession; }
    public void setAssemblyAccession(String assemblyAccession) { this.assemblyAccession = assemblyAccession; }
    public List<SubmittedSampleDto> getSamples() { return samples; }
    public void setSamples(List<SubmittedSampleDto> samples) { this.samples = samples; }
    public List<SubmittedVcfDto> getVcfs() { return vcfs; }
    public void setVcfs(List<SubmittedVcfDto> vcfs) { this.vcfs = vcfs; }
}
