package uk.ac.ebi.eva.submission.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailSender {
    private final Logger logger = LoggerFactory.getLogger(MailSender.class);
    private final JavaMailSender javaMailSender;
    private final String DEFAULT_SENDER = "eva_submissions@ebi.ac.uk";

    @Autowired
    MailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendEmail(String to, String subject, String body) {
        sendEmail(DEFAULT_SENDER, to, subject, body);
    }

    public void sendEmail(String from, String to, String subject, String body) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        try {
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Error sending mail: " + e);
        }
    }
}