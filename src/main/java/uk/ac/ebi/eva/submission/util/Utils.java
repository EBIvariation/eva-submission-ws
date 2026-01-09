package uk.ac.ebi.eva.submission.util;

import com.vdurmont.semver4j.Semver;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String extractVersionFromSchemaUrl(String schemaUrl) {
        Pattern pattern = Pattern.compile("/tags/(v\\d+\\.\\d+\\.\\d+(?:[-\\.][A-Za-z0-9]+)*)/");
        Matcher matcher = pattern.matcher(schemaUrl);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Version not found in schema URL: " + schemaUrl);
    }

    public static int compareVersions(String version1, String version2) {
        Semver s1 = new Semver(version1, Semver.SemverType.NPM);
        Semver s2 = new Semver(version2, Semver.SemverType.NPM);

        return s1.compareTo(s2);
    }
}
