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
import uk.ac.ebi.eva.submission.model.SubmissionUser;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.util.Objects;

@RestController
@RequestMapping("/v1")
public class SubmissionController extends BaseController {
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

    public SubmissionUser getUser(String bearerToken) {
        String userToken = bearerToken.replace("Bearer ", "");
        //TODO: Probably need to cache the token/UserId map
        SubmissionUser submissionUser = this.webinTokenService.getWebinUserFromToken(userToken);
        if (Objects.isNull(submissionUser)) {
            submissionUser = this.lsriTokenService.getLsriUserFromToken(userToken);
        }
        return submissionUser;
    }

    @PostMapping("submission/initiate")
    public ResponseEntity<?> initiateSubmission(@RequestHeader("Authorization") String bearerToken) {
        SubmissionUser submissionUser = this.getUser(bearerToken);
        if (Objects.isNull(submissionUser)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        Submission submission = this.submissionService.initiateSubmission(submissionUser);
        return new ResponseEntity<>(toResponseSubmission(submission), HttpStatus.OK);
    }

    @PutMapping("submission/{submissionId}/uploaded")
    public ResponseEntity<?> markSubmissionUploaded(@RequestHeader("Authorization") String bearerToken,
                                                    @PathVariable("submissionId") String submissionId) {
        SubmissionUser submissionUser = this.getUser(bearerToken);
        if (Objects.isNull(submissionUser) || !submissionService.checkUserHasAccessToSubmission(submissionUser, submissionId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Submission submission = this.submissionService.markSubmissionUploaded(submissionId);
        return new ResponseEntity<>(toResponseSubmission(submission), HttpStatus.OK);
    }

    @GetMapping("submission/{submissionId}/status")
    public ResponseEntity<?> getSubmissionStatus(@RequestHeader("Authorization") String bearerToken,
                                                 @PathVariable("submissionId") String submissionId) {
        SubmissionUser submissionUser = this.getUser(bearerToken);
        if (Objects.isNull(submissionUser) || !submissionService.checkUserHasAccessToSubmission(submissionUser, submissionId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        String submissionStatus = submissionService.getSubmissionStatus(submissionId);
       return new ResponseEntity<>(submissionStatus, HttpStatus.OK);
    }

    // TODO: admin end point (should not be exposed to the user)
    @PutMapping("submission/{submissionId}/status/{status}")
    public ResponseEntity<?> markSubmissionStatus(@RequestHeader("Authorization") String bearerToken,
                                                  @PathVariable("submissionId") String submissionId,
                                                  @PathVariable("status") SubmissionStatus status) {
        SubmissionUser submissionUser = this.getUser(bearerToken);
        if (Objects.isNull(submissionUser) || !submissionService.checkUserHasAccessToSubmission(submissionUser, submissionId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Submission submission = this.submissionService.markSubmissionStatus(submissionId, status);
        return new ResponseEntity<>(toResponseSubmission(submission), HttpStatus.OK);

    }

    // strip confidential details (e.g. user details) before returning
    private Submission toResponseSubmission(Submission submission){
        Submission responseSubmission = new Submission(submission.getSubmissionId());
        responseSubmission.setStatus(submission.getStatus());
        responseSubmission.setUploadUrl(submission.getUploadUrl());

        return responseSubmission;
    }
}
