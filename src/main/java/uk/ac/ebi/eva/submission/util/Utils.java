package uk.ac.ebi.eva.submission.util;

import uk.ac.ebi.eva.submission.entity.SubmissionAccount;

public class Utils {

    public static String getUserNameFromSubmissionAccountOrDefault(SubmissionAccount submissionAccount, String defaultName) {
        String firstName = submissionAccount.getFirstName() != null ? submissionAccount.getFirstName() : "";
        String lastName = submissionAccount.getLastName() != null ? submissionAccount.getLastName() : "";
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return defaultName;
        } else if (!firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        } else if (!firstName.isEmpty()) {
            return firstName;
        } else {
            return lastName;
        }
    }
}
