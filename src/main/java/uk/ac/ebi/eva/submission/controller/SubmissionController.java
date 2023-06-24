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
import uk.ac.ebi.eva.submission.service.WebinTokenService;

@RestController
@RequestMapping("/v1")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final WebinTokenService webinTokenService;

    public SubmissionController(SubmissionService submissionService, WebinTokenService webinTokenService) {
        this.submissionService = submissionService;
        this.webinTokenService = webinTokenService;
    }

    @PostMapping("submission/initiate/webin/{userToken}")
    public Submission initiateSubmissionWebin(@PathVariable("userToken") String userToken) {
        String userId = this.webinTokenService.getWebinUserIdFromToken(userToken);
        Submission submission = submissionService.initiateSubmission(userId);
        return submission;
    }

    @PostMapping("submission/initiate/lsri/{userToken}/")
    public String initiateSubmissionLSRI() {
        // TODO
        return "NotImplemented";
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