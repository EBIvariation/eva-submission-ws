package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.CallHomeEventEntity;
import uk.ac.ebi.eva.submission.repository.CallHomeEventRepository;
import uk.ac.ebi.eva.submission.util.SchemaDownloader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CallHomeService {
    private final Logger logger = LoggerFactory.getLogger(CallHomeService.class);

    private final CallHomeEventRepository callHomeEventRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final SchemaDownloader schemaDownloader;

    public CallHomeService(CallHomeEventRepository callHomeEventRepository, SchemaDownloader schemaDownloader) {
        this.callHomeEventRepository = callHomeEventRepository;
        this.schemaDownloader = schemaDownloader;
    }

    public boolean validateJson(JsonNode jsonPayload) {
        String latestTag = schemaDownloader.getLatestTag(SchemaDownloader.TAG_URL);
        String schemaURLWithLatestTag = schemaDownloader.getCallhomeSchemaURL().replace("{tag}", latestTag);
        String schemaContent = schemaDownloader.loadSchemaFromGitHub(schemaURLWithLatestTag);
        Schema schema = schemaRegistry.getSchema(schemaContent, InputFormat.JSON);
        List<com.networknt.schema.Error> errorList = schema.validate(jsonPayload.toString(), InputFormat.JSON,
                executionContext -> executionContext
                        .executionConfig(config -> config.formatAssertionsEnabled(true))
        );

        boolean schemaValidationPassed = errorList.isEmpty();
        if (!schemaValidationPassed) {
            logger.error("Schema validation failed: {}", errorList);
        }

        return schemaValidationPassed;
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
