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

    @PostMapping("submission/auth/lsri")
    public ResponseEntity<?> authenticateLSRI(@RequestParam("deviceCode") String deviceCode,
                                              @RequestParam("expiresIn") int codeExpirationTimeInSeconds) {
        String token = lsriTokenService.pollForToken(deviceCode, codeExpirationTimeInSeconds);
        if (Objects.nonNull(token)) {
            return new ResponseEntity<>(token, HttpStatus.OK);
        }
        return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    public String getUserId(String bearerToken){
        String userToken = bearerToken.replace("Bearer ", "");
        String userId;
        //TODO: Probably need to cache the token/UserId map
        userId = this.webinTokenService.getWebinUserIdFromToken(userToken);
        if (Objects.isNull(userId)) {
            userId = this.lsriTokenService.getLsriUserIdFromToken(userToken);
        }
        return userId;
    }

    @PostMapping("submission/initiate")
    public ResponseEntity<?> initiateSubmission(@RequestHeader("Authorization") String bearerToken) {
        String userId = this.getUserId(bearerToken);
        if (Objects.isNull(userId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        Submission submission = this.submissionService.initiateSubmission(userId);
        return new ResponseEntity<>(submission, HttpStatus.OK);
    }

    @PutMapping("submission/{submissionId}/uploaded")
    public ResponseEntity<?> markSubmissionUploaded(@RequestHeader("Authorization") String bearerToken,
                                       @PathVariable("submissionId") String submissionId) {
        String userId = this.getUserId(bearerToken);
        if (Objects.isNull(userId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        // TODO: Confirm that this userId has access to the submission
        Submission submission = this.submissionService.markSubmissionUploaded(submissionId);
        if (Objects.nonNull(submission)) {
            return new ResponseEntity<>(submission, HttpStatus.OK);
        }else {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("submission/{submissionId}/status")
    public ResponseEntity<?> getSubmissionStatus(@RequestHeader("Authorization") String bearerToken,
                                      @PathVariable("submissionId") String submissionId) {
        String userId = this.getUserId(bearerToken);
        if (Objects.isNull(userId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        // TODO: Confirm that this userId has access to the submission
        String submissionStatus = submissionService.getSubmissionStatus(submissionId);
        if (Objects.nonNull(submissionStatus)) {
            return new ResponseEntity<>(submissionStatus, HttpStatus.OK);
        }else {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }

    // TODO: admin end point (should not be exposed to the user)
    @PutMapping("submission/{submissionId}/status/{status}")
    public ResponseEntity<?> markSubmissionStatus(@RequestHeader("Authorization") String bearerToken,
                                           @PathVariable("submissionId") String submissionId,
                                           @PathVariable("status") SubmissionStatus status) {
        String userId = this.getUserId(bearerToken);
        if (Objects.isNull(userId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        // TODO: Confirm that this userId has access to the submission
        Submission submission = this.submissionService.markSubmissionStatus(submissionId, status);
        if (Objects.nonNull(submission)) {
            return new ResponseEntity<>(submission, HttpStatus.OK);
        }else {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }
    }
}
