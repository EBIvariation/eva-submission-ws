package uk.ac.ebi.eva.submission.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(schema = "eva_submissions", name = "submission_details")
@TypeDef(typeClass = JsonType.class, defaultForType = JsonNode.class)
public class SubmissionDetails {
    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "submission_id", referencedColumnName = "submission_id")
    private Submission submission;

    @Column(nullable = false, name = "project_title")
    private String projectTitle;

    @Column(nullable = false, name = "project_description")
    private String projectDescription;

    @Column(columnDefinition = "jsonb", name = "metadata_json", nullable = false)
    private JsonNode metadataJson;

    public SubmissionDetails() {
    }

    public SubmissionDetails(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public JsonNode getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(JsonNode metadataJson) {
        this.metadataJson = metadataJson;
    }
}
