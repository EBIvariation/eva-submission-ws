package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionAccountRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionDetailsRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.util.EmailNotificationHelper;
import uk.ac.ebi.eva.submission.util.HTMLHelper;
import uk.ac.ebi.eva.submission.util.MailSender;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    private final SubmissionAccountRepository submissionAccountRepository;

    private final SubmissionDetailsRepository submissionDetailsRepository;

    private final GlobusDirectoryProvisioner globusDirectoryProvisioner;

    private final MailSender mailSender;

    @Value("${globus.uploadHttpDomain}")
    private String uploadHttpDomain;

    private EmailNotificationHelper emailHelper;

    public SubmissionService(SubmissionRepository submissionRepository,
                             SubmissionAccountRepository submissionAccountRepository,
                             SubmissionDetailsRepository submissionDetailsRepository,
                             GlobusDirectoryProvisioner globusDirectoryProvisioner,
                             MailSender mailSender, EmailNotificationHelper emailHelper) {
        this.submissionRepository = submissionRepository;
        this.submissionAccountRepository = submissionAccountRepository;
        this.submissionDetailsRepository = submissionDetailsRepository;
        this.globusDirectoryProvisioner = globusDirectoryProvisioner;
        this.mailSender = mailSender;
        this.emailHelper = emailHelper;
    }

    public Submission initiateSubmission(SubmissionAccount submissionAccount) {
        String submissionId = UUID.randomUUID().toString();
        String directoryToCreate = String.format("%s/%s", submissionAccount.getId(), submissionId);
        globusDirectoryProvisioner.createSubmissionDirectory(directoryToCreate);

        Optional<SubmissionAccount> optSubmissionAccount = submissionAccountRepository.findById(submissionAccount.getId());
        // if the user account is not present or if its primary email has changed, save/update the user account
        if (!optSubmissionAccount.isPresent() || !optSubmissionAccount.get().getPrimaryEmail().equals(submissionAccount.getPrimaryEmail())) {
            submissionAccountRepository.save(submissionAccount);
        }

        Submission submission = new Submission(submissionId);
        submission.setSubmissionAccount(submissionAccount);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submission.setUploadUrl(uploadHttpDomain + "/" + directoryToCreate);

        return submissionRepository.save(submission);
    }

    public Submission markSubmissionUploaded(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        submission.setStatus(SubmissionStatus.UPLOADED.toString());
        submission.setUploadedTime(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    public String getSubmissionStatus(String submissionId) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        return submission.getStatus();
    }

    public Submission markSubmissionStatus(String submissionId, SubmissionStatus status) {
        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        submission.setStatus(status.toString());
        if (status == SubmissionStatus.COMPLETED) {
            submission.setCompletionTime(LocalDateTime.now());
        }

        return submissionRepository.save(submission);
    }

    public boolean checkUserHasAccessToSubmission(SubmissionAccount account, String submissionId) {
        Optional<Submission> optSubmission = submissionRepository.findById(submissionId);
        if (optSubmission.isPresent()) {
            SubmissionAccount submissionAccount = optSubmission.get().getSubmissionAccount();
            return submissionAccount.getId().equals(account.getId());
        } else {
            throw new SubmissionDoesNotExistException("Given submission with id " + submissionId + " does not exist");
        }
    }

    public void sendMailNotificationForStatusUpdate(SubmissionAccount submissionAccount, String submissionId,
                                                    SubmissionStatus submissionStatus, boolean success) {
        String sendTo = submissionAccount.getPrimaryEmail();
        String subject = emailHelper.getSubjectForSubmissionStatusUpdate(submissionStatus, success);
        String body = emailHelper.getTextForSubmissionStatusUpdate(submissionAccount, submissionId, submissionStatus, success);
        mailSender.sendEmail(sendTo, subject, body);
    }
}
