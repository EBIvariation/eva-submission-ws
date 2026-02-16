package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.CallHomeEventEntity;
import uk.ac.ebi.eva.submission.repository.CallHomeEventRepository;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CallHomeService {
    private final CallHomeEventRepository callHomeEventRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public CallHomeService(CallHomeEventRepository callHomeEventRepository) {
        this.callHomeEventRepository = callHomeEventRepository;
    }

    public void registerCallHomeEvent(JsonNode callHomeEventJson) {
        CallHomeEventEntity callHomeEventEntity = getCallHomeEventEntity(callHomeEventJson);
        callHomeEventRepository.save(callHomeEventEntity);
    }

    private CallHomeEventEntity getCallHomeEventEntity(JsonNode callHomeEventJson) {
        CallHomeEventEntity callHomeEventEntity = new CallHomeEventEntity();

        callHomeEventEntity.setDeploymentId(get(callHomeEventJson, "deploymentId", String.class));
        callHomeEventEntity.setRunId(get(callHomeEventJson, "runId", String.class));
        callHomeEventEntity.setEventType(get(callHomeEventJson, "eventType", String.class));
        callHomeEventEntity.setCliVersion(get(callHomeEventJson, "cliVersion", String.class));
        callHomeEventEntity.setCreatedAt(get(callHomeEventJson, "createdAt", LocalDateTime.class));
        callHomeEventEntity.setRuntimeSeconds(get(callHomeEventJson, "runtimeSeconds", Integer.class));
        callHomeEventEntity.setExecutor(get(callHomeEventJson, "executor", String.class));
        callHomeEventEntity.setTasks(get(callHomeEventJson, "tasks", String.class));
        callHomeEventEntity.setRawPayload(callHomeEventJson);

        return callHomeEventEntity;
    }

    private <T> T get(JsonNode node, String field, Class<T> type) {
        if (node == null || field == null) {
            return null;
        }

        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }

        try {
            if (v.isArray() && type == String.class) {
                String joined = StreamSupport.stream(v.spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.joining(","));
                return type.cast(joined);
            }
            return MAPPER.convertValue(v, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
