package uk.ac.ebi.eva.submission.exception;

public class RequiredFieldsMissingException extends RuntimeException {
    public RequiredFieldsMissingException(String msg) {
        super(msg);
    }
}
