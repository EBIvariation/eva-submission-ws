package uk.ac.ebi.eva.submission.exception;

public class SubmissionDoesNotExistException extends RuntimeException {

    public SubmissionDoesNotExistException(String submissionId) {
        super("Submission with id " + submissionId + " does not exist");
    }
}
