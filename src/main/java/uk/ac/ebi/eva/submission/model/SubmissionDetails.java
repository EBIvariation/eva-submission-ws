package uk.ac.ebi.eva.submission.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "submission_details")
public class SubmissionDetails {
    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @OneToOne
    @PrimaryKeyJoinColumn(name = "submission_id", referencedColumnName = "submission_id")
    private Submission submission;

    @Column(nullable = false, name = "project_alias")
    private String projectAlias;

    @Column(nullable = false, name = "description")
    private String description;

    public SubmissionDetails() {

    }

    public SubmissionDetails(Submission submission){
        this.submission = submission;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public Submission getSubmission() {
        return submission;
    }

    public String getProjectAlias() {
        return projectAlias;
    }

    public void setProjectAlias(String projectAlias) {
        this.projectAlias = projectAlias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
