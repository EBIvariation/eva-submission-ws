package uk.ac.ebi.eva.submission.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.SubmissionService;

@RestController
@RequestMapping("/v1")
public class SubmissionController {
    private SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("submission/initiate")
    public String initiateSubmission() {
        Submission submission = submissionService.initiateSubmission();
        return submission.getSubmissionId();
    }

    @PutMapping("submission/{submissionId}/uploaded")
    public void markSubmissionUploaded(@PathVariable("submissionId") String submissionId) {
        submissionService.markSubmissionUploaded(submissionId);
    }

    @GetMapping("submission/{submissionId}/status")
    public String getSubmissionStatus(@PathVariable("submissionId") String submissionId) {
        return submissionService.getSubmissionStatus(submissionId).toString();
    }

    // TODO: admin end point (should not be exposed to the user)
    @PutMapping("submission/{submissionId}/status/{status}")
    public Submission markSubmissionStatus(@PathVariable("submissionId") String submissionId, @PathVariable("status") SubmissionStatus status) {
        return submissionService.markSubmissionStatus(submissionId, status);
    }
}