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
    }

    @Test
    public void testGetSubjectForSubmissionStatusUpdate() {
        String expectedSubject = "EVA Submission Update: UPLOADED SUCCESS";
        String actualSubject = emailNotificationHelper.getSubjectForSubmissionStatusUpdate(SubmissionStatus.UPLOADED, Boolean.TRUE);

        assertEquals(expectedSubject, actualSubject);
    }

    @Test
    public void testGetTextForSubmissionStatusUpdateSuccess() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear John," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "submission id: 12345<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:green;\">SUCCESS</span></b>" +
                "<br /><br /><br />" +
                "<span style=\"font-size:10px;\">Please don't reply to this email.</span><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualText = emailNotificationHelper.getTextForSubmissionStatusUpdate(submissionAccount,
                "12345", SubmissionStatus.UPLOADED, Boolean.TRUE);

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetTextForSubmissionStatusUpdateFailure() {
        SubmissionAccount submissionAccount = new SubmissionAccount("johndoe@example.com",
                LoginMethod.WEBIN.toString(), "John", "Doe", "john@example.com");
        String expectedText = "Dear John," +
                "<br /><br />" +
                "Here is the update for your submission: " +
                "<br /><br />" +
                "submission id: 12345<br />" +
                "Submission Status: UPLOADED<br />" +
                "Result: <b><span style=\"color:red;\">FAILED</span></b>" +
                "<br /><br /><br />" +
                "<span style=\"font-size:10px;\">Please don't reply to this email.</span><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualText = emailNotificationHelper.getTextForSubmissionStatusUpdate(submissionAccount,
                "12345", SubmissionStatus.UPLOADED, Boolean.FALSE);

        assertEquals(expectedText, actualText);
    }

    @Test
    public void testGetNotificationFooter(){
        String expectedFooter = "<span style=\"font-size:10px;\">Please don't reply to this email.</span><br />" +
                "<span style=\"font-size:10px;\">For any issues/support please contact us at </span>" +
                "<span style=\"font-size:10px;\"> <a href=\"mailto:eva-helpdesk@ebi.ac.uk\">eva-helpdesk@ebi.ac.uk</a> " +
                "</span><br /><span style=\"font-size:10px;\">European Variation Archive: EMBL-EBI</span>";

        String actualFooter = emailNotificationHelper.getNotificationFooter();

        assertEquals(expectedFooter, actualFooter);
    }

}

