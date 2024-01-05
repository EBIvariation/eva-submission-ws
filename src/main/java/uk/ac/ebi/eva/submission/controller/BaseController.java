package uk.ac.ebi.eva.submission.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;

public class BaseController {

    @ExceptionHandler(SubmissionDoesNotExistException.class)
    public ResponseEntity<?> handleException(SubmissionDoesNotExistException submissionDoesNotExistException) {
        return new ResponseEntity<>(submissionDoesNotExistException.getMessage(), HttpStatus.NOT_FOUND);
    }
}
