package uk.ac.ebi.eva.submission.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public class SubmissionTrackingDetailsDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    private String projectAccession;

    private List<String> analysisAccessions;

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

    public List<String> getAnalysisAccessions() {
        return analysisAccessions;
    }

    public void setAnalysisAccessions(List<String> analysisAccessions) {
        this.analysisAccessions = analysisAccessions;
    }
}
