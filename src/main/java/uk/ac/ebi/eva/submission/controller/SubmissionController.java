package uk.ac.ebi.eva.submission.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.SubmissionService;

@RestController
@RequestMapping("/v1")
public class SubmissionController {
    private SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("ftp")
    public String createFtpDirectory() {
        SubmissionStatus submissionStatus = submissionService.createFtpDirectory();
        return submissionStatus.getSubmissionId();
    }

    @PutMapping("completed")
    public String markSubmissionComplete(@RequestBody String submissionId) {
        return submissionService.markSubmissionComplete(submissionId).toString();
    }
}