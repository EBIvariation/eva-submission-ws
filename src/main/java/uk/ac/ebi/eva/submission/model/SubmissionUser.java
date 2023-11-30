package uk.ac.ebi.eva.submission.model;

import com.google.common.base.Objects;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "submission_user", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "login_type"})})
public class SubmissionUser {
    @Id
    private String id;

    @NonNull
    @Column(nullable = false, name = "user_id")
    private String userId;

    @NonNull
    @Column(nullable = false, name = "login_type")
    private String loginType;

    @NonNull
    @Column(nullable = false, name = "email_id")
    private String emailId;

    @NonNull
    @Column(nullable = false, name = "first_name")
    private String firstName;

    @NonNull
    @Column(nullable = false, name = "last_name")
    private String lastName;

    public SubmissionUser() {

    }

    public SubmissionUser(String userId, String loginType) {
        this.id = getId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
    }

    public SubmissionUser(String userId, String loginType, String firstName, String lastName, String emailId) {
        this.id = getId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.emailId = emailId;
    }

    public String getId() {
        return id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    @NonNull
    public String getLoginType() {
        return loginType;
    }

    @NonNull
    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(@NonNull String emailId) {
        this.emailId = emailId;
    }

    @NonNull
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NonNull String firstName) {
        this.firstName = firstName;
    }

    @NonNull
    public String getLastName() {
        return lastName;
    }

    public void setLastName(@NonNull String lastName) {
        this.lastName = lastName;
    }

    private String getId(String userId, String loginType) {
        return userId + "_" + loginType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionUser)) return false;
        SubmissionUser user = (SubmissionUser) o;
        return Objects.equal(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}