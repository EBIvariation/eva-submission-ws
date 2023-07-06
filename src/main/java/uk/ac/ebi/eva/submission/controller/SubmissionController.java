package uk.ac.ebi.eva.submission.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.util.Objects;

@RestController
@RequestMapping("/v1")
public class SubmissionController {
    private final SubmissionService submissionService;
    private final WebinTokenService webinTokenService;
    private final LsriTokenService lsriTokenService;

    public SubmissionController(SubmissionService submissionService, WebinTokenService webinTokenService,
                                LsriTokenService lsriTokenService) {
        this.submissionService = submissionService;
        this.webinTokenService = webinTokenService;
        this.lsriTokenService = lsriTokenService;
    }

    private ResponseEntity<?> initiateSubmission(String userId) {
        if (Objects.nonNull(userId)) {
            Submission submission = this.submissionService.initiateSubmission(userId);
            if (Objects.nonNull(submission)) {
                return new ResponseEntity<>(submission, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    @PostMapping("submission/initiate/webin")
    public ResponseEntity<?> initiateSubmissionWebin(@RequestHeader("Authorization") String bearerToken) {
        String userToken = bearerToken.replace("Bearer ", "");
        return this.initiateSubmission(this.webinTokenService.getWebinUserIdFromToken(userToken));
    }

    @PostMapping("submission/initiate/lsri")
    public ResponseEntity<?> initiateSubmissionLSRI(@RequestParam("deviceCode") String deviceCode,
                                                    @RequestParam("expiresIn") int codeExpirationTimeInSeconds) {
        return this.initiateSubmission(this.lsriTokenService.getLsriUserIdFromToken(deviceCode,
                                                                                    codeExpirationTimeInSeconds));
    }

    @PutMapping("submission/{submissionId}/uploaded")
    public void markSubmissionUploaded(@PathVariable("submissionId") String submissionId) {
        submissionService.markSubmissionUploaded(submissionId);
    }

    @GetMapping("submission/{submissionId}/status")
    public String getSubmissionStatus(@PathVariable("submissionId") String submissionId) {
        return submissionService.getSubmissionStatus(submissionId);
    }

    // TODO: admin end point (should not be exposed to the user)
    @PutMapping("submission/{submissionId}/status/{status}")
    public Submission markSubmissionStatus(@PathVariable("submissionId") String submissionId, @PathVariable("status") SubmissionStatus status) {
        return submissionService.markSubmissionStatus(submissionId, status);
    }
}
