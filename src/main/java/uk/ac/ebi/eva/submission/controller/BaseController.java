package uk.ac.ebi.eva.submission.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BaseController {
    private final WebinTokenService webinTokenService;
    private final LsriTokenService lsriTokenService;

    public BaseController(WebinTokenService webinTokenService, LsriTokenService lsriTokenService) {
        this.webinTokenService = webinTokenService;
        this.lsriTokenService = lsriTokenService;
    }

    @ExceptionHandler(SubmissionDoesNotExistException.class)
    public ResponseEntity<?> handleException(SubmissionDoesNotExistException submissionDoesNotExistException) {
        return new ResponseEntity<>(submissionDoesNotExistException.getMessage(), HttpStatus.NOT_FOUND);
    }

    public SubmissionAccount getSubmissionAccount(String bearerToken) {
        String userToken = bearerToken.replace("Bearer ", "");
        //TODO: Probably need to cache the token/UserId map
        SubmissionAccount submissionAccount = this.webinTokenService.getWebinUserAccountFromToken(userToken);
        if (Objects.isNull(submissionAccount)) {
            submissionAccount = this.lsriTokenService.getLsriUserAccountFromToken(userToken);
        }
        return submissionAccount;
    }

    public List<Submission> stripUserDetails(List<Submission> submission){
        return submission.stream().map(this::stripUserDetails).collect(Collectors.toList());
    }

    public Submission stripUserDetails(Submission submission) {
        Submission responseSubmission = new Submission(submission.getSubmissionId());
        responseSubmission.setStatus(submission.getStatus());
        responseSubmission.setUploadUrl(submission.getUploadUrl());
        responseSubmission.setInitiationTime(submission.getInitiationTime());
        responseSubmission.setUploadedTime(submission.getUploadedTime());
        responseSubmission.setCompletionTime(submission.getCompletionTime());
        return responseSubmission;
    }
}
