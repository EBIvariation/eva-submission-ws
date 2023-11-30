package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.model.SubmissionUser;

public interface SubmissionUserRepository extends CrudRepository<SubmissionUser, String> {

}