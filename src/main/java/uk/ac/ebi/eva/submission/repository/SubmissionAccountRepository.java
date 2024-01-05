package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.model.SubmissionAccount;

public interface SubmissionAccountRepository extends CrudRepository<SubmissionAccount, String> {

}