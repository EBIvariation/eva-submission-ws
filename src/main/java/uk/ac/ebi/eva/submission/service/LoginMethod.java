package uk.ac.ebi.eva.submission.service;

public enum LoginMethod {
    WEBIN("webin", "submissionAccountId", "firstName", "surname", "emailAddress"),
    LSRI("lsri", "sub", "given_name", "family_name", "email");

    String loginType;
    String userIdToken;
    String firstNameToken;
    String lastNameToken;
    String emailIdToken;

    LoginMethod(String loginType, String userIdToken, String firstNameToken, String lastNameToken, String emailIdToken) {
        this.loginType = loginType;
        this.userIdToken = userIdToken;
        this.firstNameToken = firstNameToken;
        this.lastNameToken = lastNameToken;
        this.emailIdToken = emailIdToken;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getUserIdToken() {
        return userIdToken;
    }

    public String getFirstNameToken() {
        return firstNameToken;
    }

    public String getLastNameToken() {
        return lastNameToken;
    }

    public String getEmailIdToken() {
        return emailIdToken;
    }
}
