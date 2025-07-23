package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.LoginMethod;
import uk.ac.ebi.eva.submission.util.EmailNotificationHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EmailNotificationHelperTest {
    private EmailNotificationHelper emailNotificationHelper;

    @BeforeEach
    public void setup() {
        emailNotificationHelper = new EmailNotificationHelper();
        emailNotificationHelper.setEvaHelpdeskEmail("eva-helpdesk@ebi.ac.uk");
        emailNotificationHelper.setEvaConsentStatement("https://docs.google.com/document/d/1UaRmimAe919IZYIH55mDXMe-4sHsRVqaP4moqx2IYE4");
    }

    @Test
    public void testGetSubjectForSubmissionStatusUpdate() {
        String expectedSubject = "EVA Submission Update: UPLOADED SUCCESS";
        String actualSubject = emailNotificationHelper.getSubjectForSubmissionStatusUpdate(SubmissionStatus.UPLOADED, true);

        assertEquals(expectedSubject, actualSubject);
    }

    @Test
    public void testGetTextForSubmissionStatusUpdateSuccess() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear John Doe," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "Submission ID: submission12345<br />" +
                "Project Title: project123<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:green;\">SUCCESS</span></b>" +
                "<br /><br /><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualText = emailNotificationHelper.getTextForSubmissionStatusUpdate(submissionAccount,
                "submission12345", "project123", SubmissionStatus.UPLOADED, false, true);

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetTextForSubmissionStatusUpdateSuccess_withHumanTaxonomy() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear John Doe," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "Submission ID: submission12345<br />" +
                "Project Title: project123<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:green;\">SUCCESS</span></b>" +
                "<br /><br /><br />" +
                "<b><span style=\"color:black;\">" +
                "Note: </span></b>Your submission contains human genotypes for which we require a Consent Statement. " +
                "Please copy the template provided in the link below, fill it, sign it and return it to <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a>. " +
                "For more info, please see our <a href=\"https://www.ebi.ac.uk/eva/?Help#submissionPanel&link=consent-statement-for-human-genotype-data\">help</a> section." +
                "<br /><br /><a href=\"https://docs.google.com/document/d/1UaRmimAe919IZYIH55mDXMe-4sHsRVqaP4moqx2IYE4\">Link to Consent Statement</a><br /><br /><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> </span><br />" +
                "<span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualText = emailNotificationHelper.getTextForSubmissionStatusUpdate(submissionAccount,
                "submission12345", "project123", SubmissionStatus.UPLOADED, true, true);

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetTextForSubmissionStatusUpdateFailure() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear John Doe," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "Submission ID: 12345<br />" +
                "Project Title: project123<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:red;\">FAILED</span></b>" +
                "<br /><br /><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualText = emailNotificationHelper.getTextForSubmissionStatusUpdate(submissionAccount,
                "12345", "project123", SubmissionStatus.UPLOADED, false, false);

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetTextForEVAHelpdeskSubmissionUploaded() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear EVA Helpdesk," +
                "<br /><br />" +
                "The user has uploaded a new Submission: " +
                "<br /><br />" +
                "submission ID: submission-12345<br />" +
                "Project Title: Test Project Title<br />" +
                "User Name: John Doe<br />" +
                "User Email: john@example.com" +
                "<br /><br /><br />" +
                "<span style=\"font-size:10px;\">Please don't reply to this email.</span><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> </span><br />" +
                "<span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";
        String actualText = emailNotificationHelper.getTextForEVAHelpdeskSubmissionUploaded(submissionAccount,
                "submission-12345", "Test Project Title");

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetNotificationFooter() {
        String expectedFooter = "<span style=\"font-size:10px;\">Please don't reply to this email.</span><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualFooter = emailNotificationHelper.getNotificationFooter(true);

        assertEquals(expectedFooter, actualFooter);
    }

}

