package uk.ac.ebi.eva.submission.model;

import java.util.List;

public class WebinUserInfo {
    private String submissionAccountId;
    private String centerName;
    private String fullCenterName;
    private String brokerName;
    private String laboratoryName;
    private String country;
    private String address;
    private Boolean metagenomeSubmitter;
    private Boolean metagenomicsAnalysis;
    private Boolean suspended;

    private List<WebinSubmissionContact> submissionContacts;

    public String getSubmissionAccountId() {
        return submissionAccountId;
    }

    public void setSubmissionAccountId(String submissionAccountId) {
        this.submissionAccountId = submissionAccountId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getFullCenterName() {
        return fullCenterName;
    }

    public void setFullCenterName(String fullCenterName) {
        this.fullCenterName = fullCenterName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getLaboratoryName() {
        return laboratoryName;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getMetagenomeSubmitter() {
        return metagenomeSubmitter;
    }

    public void setMetagenomeSubmitter(Boolean metagenomeSubmitter) {
        this.metagenomeSubmitter = metagenomeSubmitter;
    }

    public Boolean getMetagenomicsAnalysis() {
        return metagenomicsAnalysis;
    }

    public void setMetagenomicsAnalysis(Boolean metagenomicsAnalysis) {
        this.metagenomicsAnalysis = metagenomicsAnalysis;
    }

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public List<WebinSubmissionContact> getSubmissionContacts() {
        return submissionContacts;
    }

    public void setSubmissionContacts(List<WebinSubmissionContact> submissionContacts) {
        this.submissionContacts = submissionContacts;
    }
}