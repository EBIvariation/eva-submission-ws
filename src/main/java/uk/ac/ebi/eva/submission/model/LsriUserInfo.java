package uk.ac.ebi.eva.submission.model;

import com.google.gson.annotations.SerializedName;

public class LsriUserInfo {
    @SerializedName("sub")
    private String UserId;
    @SerializedName("given_name")
    private String firstName;
    @SerializedName("family_name")
    private String lastName;
    private String email;

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
