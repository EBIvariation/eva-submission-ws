package uk.ac.ebi.eva.submission.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public class SubmissionTrackingDetailsDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;
    private String projectAccession;
    private String projectTitle;
    private String projectTaxonomies;
    private String metadataJsonFile;
    private List<SubmittedAnalysisDto> analyses;
    private URI rtLink;

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getProjectAccession() {
        return projectAccession;
    }

    public void setProjectAccession(String projectAccession) {
        this.projectAccession = projectAccession;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProjectTaxonomies() {
        return projectTaxonomies;
    }

    public void setProjectTaxonomies(String projectTaxonomies) {
        this.projectTaxonomies = projectTaxonomies;
    }

    public String getMetadataJsonFile() {
        return metadataJsonFile;
    }

    public void setMetadataJsonFile(String metadataJsonFile) {
        this.metadataJsonFile = metadataJsonFile;
    }

    public List<SubmittedAnalysisDto> getAnalyses() {
        return analyses;
    }

    public void setAnalyses(List<SubmittedAnalysisDto> analyses) {
        this.analyses = analyses;
    }

    public URI getRtLink() {
        return rtLink;
    }

    public void setRtLink(URI rtLink) {
        this.rtLink = rtLink;
    }
}
