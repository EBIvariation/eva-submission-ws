package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;

public interface SubmissionAccountRepository extends CrudRepository<SubmissionAccount, String> {

}