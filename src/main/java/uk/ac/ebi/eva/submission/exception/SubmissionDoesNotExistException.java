package uk.ac.ebi.eva.submission.exception;

public class SubmissionDoesNotExistException extends RuntimeException{
   public SubmissionDoesNotExistException(){
        super("Submission does not exist");
    }
    public SubmissionDoesNotExistException(String msg){
        super(msg);
    }
}
