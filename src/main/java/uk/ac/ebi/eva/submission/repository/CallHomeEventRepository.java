package uk.ac.ebi.eva.submission.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.eva.submission.entity.CallHomeEventEntity;

public interface CallHomeEventRepository extends CrudRepository<CallHomeEventEntity, Long> {
}
