package uk.ac.ebi.eva.submission.model;

import com.google.common.base.Objects;
import org.springframework.lang.NonNull;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.List;

@Entity
@Table(schema = "eva_submissions", name = "submission_account", uniqueConstraints = {@UniqueConstraint(columnNames =
        {"user_id", "login_type"})})
public class SubmissionAccount {

    @Id
    private String id;

    @NonNull
    @Column(nullable = false, name = "user_id")
    private String userId;

    @NonNull
    @Column(nullable = false, name = "login_type")
    private String loginType;

    @NonNull
    @Column(nullable = false, name = "primary_email")
    private String primaryEmail;

    @ElementCollection
    @Column(name = "secondary_emails")
    private List<String> secondaryEmails;

    @NonNull
    @Column(nullable = false, name = "first_name")
    private String firstName;

    @NonNull
    @Column(nullable = false, name = "last_name")
    private String lastName;

    public SubmissionAccount() {

    }

    public SubmissionAccount(String userId, String loginType) {
        this.id = createId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
    }

    public SubmissionAccount(String userId, String loginType, String firstName, String lastName, String primaryEmail) {
        this.id = createId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.primaryEmail = primaryEmail;
    }

    public SubmissionAccount(String userId, String loginType, String firstName, String lastName, String primaryEmail,
                             List<String> secondaryEmails) {
        this.id = createId(userId, loginType);
        this.userId = userId;
        this.loginType = loginType;
        this.firstName = firstName;
        this.lastName = lastName;
        this.primaryEmail = primaryEmail;
        this.secondaryEmails = secondaryEmails;
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
    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(@NonNull String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public List<String> getSecondaryEmails() {
        return secondaryEmails;
    }

    public void setSecondaryEmails(List<String> secondaryEmails) {
        this.secondaryEmails = secondaryEmails;
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

    private String createId(String userId, String loginType) {
        return userId + "_" + loginType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionAccount)) return false;
        SubmissionAccount account = (SubmissionAccount) o;
        return Objects.equal(getId(), account.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}