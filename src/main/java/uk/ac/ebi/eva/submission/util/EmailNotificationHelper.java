package uk.ac.ebi.eva.submission.util;

import org.springframework.stereotype.Component;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;

@Component
public class EmailNotificationHelper {
    public static final String EVA_HELPDESK_EMAIL = "eva-helpdesk@ebi.ac.uk";

    public String getSubjectForSubmissionStatusUpdate(SubmissionStatus submissionStatus, boolean success) {
        String result = (success == true) ? "SUCCESS" : "FAILED";
        return String.format("EVA Submission Update: %s %s", submissionStatus, result);
    }

    public String getTextForSubmissionStatusUpdate(SubmissionAccount submissionAccount, String submissionId,
                                                   SubmissionStatus submissionStatus, boolean success) {
        String result;
        String resultColor;
        if (success) {
            result = "SUCCESS";
            resultColor = "green";
        } else {
            result = "FAILED";
            resultColor = "red";
        }

        String notificationText = new HTMLHelper()
                .addText("Dear " + submissionAccount.getFirstName() + ",")
                .addGap(1)
                .addText("Here is the update for your submission: ")
                .addGap(1)
                .addText("submission ID: " + submissionId)
                .addLineBreak()
                .addText("Submission Status: " + submissionStatus)
                .addLineBreak()
                .addText("Result: ")
                .addBoldTextWithColor(result, resultColor)
                .addGap(2)
                .build();

        notificationText += getNotificationFooter();

        return notificationText;
    }

    public String getTextForEVAHelpdeskSubmissionUploaded(String submissionId, String projectTitle) {
        String notificationText = new HTMLHelper()
                .addText("Dear EVA Helpdesk,")
                .addGap(1)
                .addText("The user has uploaded a new Submission: ")
                .addGap(1)
                .addText("submission ID: " + submissionId)
                .addLineBreak()
                .addText("Project Title: " + projectTitle)
                .addGap(2)
                .build();

        notificationText += getNotificationFooter();

        return notificationText;
    }

    public String getNotificationFooter() {
        return new HTMLHelper()
                .addTextWithSize("Please don't reply to this email.", 10)
                .addLineBreak()
                .addTextWithSize("For any issues/support please contact us at ", 10)
                .addEmailLinkWithSize(EVA_HELPDESK_EMAIL, EVA_HELPDESK_EMAIL, 10)
                .addLineBreak()
                .addTextWithSize("European Variation Archive: EMBL-EBI", 10)
                .build();

    }
}
