package uk.ac.ebi.eva.submission.exception;

public class UnsupportedVersionException extends RuntimeException {
    public UnsupportedVersionException(String version) {
        super("You are using version " + version + " of eva-sub-cli which is not supported. " +
                "Please upgrade to the latest version to avoid future submission failures.");
    }
}