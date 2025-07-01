package uk.ac.ebi.eva.submission.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;

import static uk.ac.ebi.eva.submission.util.Utils.getUserNameFromSubmissionAccountOrDefault;

@Component
public class EmailNotificationHelper {
    @Value("${eva.helpdesk.email}")
    private String evaHelpdeskEmail;

    @Value("${eva.consent.statement}")
    private String evaConsentStatement;

    public String getSubjectForSubmissionStatusUpdate(SubmissionStatus submissionStatus, boolean success) {
        String result = success ? "SUCCESS" : "FAILED";
        return String.format("EVA Submission Update: %s %s", submissionStatus, result);
    }

    public String getTextForSubmissionStatusUpdate(SubmissionAccount submissionAccount, String submissionId,
                                                   String projectTitle, SubmissionStatus submissionStatus,
                                                   boolean needConsentStatement, boolean success) {
        String result;
        String resultColor;
        String notificationText;
        if (success) {
            result = "SUCCESS";
            resultColor = "green";
        } else {
            result = "FAILED";
            resultColor = "red";
        }

        HTMLHelper htmlHelper = new HTMLHelper()
                .addText("Dear " + getUserNameFromSubmissionAccountOrDefault(submissionAccount, "User") + ",")
                .addGap(1)
                .addText("Here is the update for your submission: ")
                .addGap(1)
                .addText("Submission ID: " + submissionId)
                .addLineBreak()
                .addText("Project Title: " + projectTitle)
                .addLineBreak()
                .addText("Submission Status: " + submissionStatus)
                .addLineBreak()
                .addText("Result: ")
                .addBoldTextWithColor(result, resultColor);

        if (needConsentStatement) {
            htmlHelper
                    .addGap(2)
                    .addBoldTextWithColor("Note: ", "black")
                    .addText("Your submission contains human genotypes for which we require a Consent Statement. " +
                            "Please copy the template provided in the link below, fill it, sign it and return it to ")
                    .addEmailLink(evaHelpdeskEmail, evaHelpdeskEmail)
                    .addGap(1)
                    .addLink(evaConsentStatement, "Link to Consent Statement");
        }

        notificationText = htmlHelper.addGap(2).build();
        notificationText += getNotificationFooter(false);

        return notificationText;
    }

    public String getTextForEVAHelpdeskSubmissionUploaded(SubmissionAccount submissionAccount, String submissionId,
                                                          String projectTitle) {
        String notificationText = new HTMLHelper()
                .addText("Dear EVA Helpdesk,")
                .addGap(1)
                .addText("The user has uploaded a new Submission: ")
                .addGap(1)
                .addText("submission ID: " + submissionId)
                .addLineBreak()
                .addText("Project Title: " + projectTitle)
                .addLineBreak()
                .addText("User Name: " + getUserNameFromSubmissionAccountOrDefault(submissionAccount, "Not Set"))
                .addLineBreak()
                .addText("User Email: " + submissionAccount.getPrimaryEmail())
                .addGap(2)
                .build();

        notificationText += getNotificationFooter(true);

        return notificationText;
    }

    public String getNotificationFooter(boolean addDontReply) {
        HTMLHelper htmlHelper = new HTMLHelper();
        if (addDontReply) {
            htmlHelper.addTextWithSize("Please don't reply to this email.", 10)
                    .addLineBreak();
        }

        htmlHelper.addTextWithSize("For any issues/support please contact us at ", 10)
                .addEmailLinkWithSize(evaHelpdeskEmail, evaHelpdeskEmail, 10)
                .addLineBreak()
                .addTextWithSize("European Variation Archive: EMBL-EBI", 10)
                .build();

        return htmlHelper.build();

    }

    public String getEvaHelpdeskEmail() {
        return evaHelpdeskEmail;
    }

    public void setEvaHelpdeskEmail(String evaHelpdeskEmail) {
        this.evaHelpdeskEmail = evaHelpdeskEmail;
    }
}
