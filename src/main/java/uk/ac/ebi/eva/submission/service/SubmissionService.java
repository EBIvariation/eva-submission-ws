package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class SubmissionService {

    private SubmissionRepository submissionRepository;

    @Value("${eva.submission.dropbox}")
    private String submissionDropbox;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public Submission initiateSubmission() {
        String submissionId = createSubmissionDirectory();

        Submission submission = new Submission(submissionId);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());

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

    private String createSubmissionDirectory() {
        String submissionId = UUID.randomUUID().toString();

        try {
            Path dirPath = Paths.get(submissionDropbox + "/" + submissionId);
            if (!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);

                Set<PosixFilePermission> filePermissions = new HashSet<>();
                filePermissions.add(PosixFilePermission.OWNER_READ);
                filePermissions.add(PosixFilePermission.OWNER_WRITE);
                filePermissions.add(PosixFilePermission.GROUP_WRITE);

                Files.setPosixFilePermissions(dirPath, filePermissions);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error Initiating Submission " + e);
        }

        return submissionId;
    }
}
