package uk.ac.ebi.eva.submission.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.eva.submission.entity.CallHomeEventEntity;
import uk.ac.ebi.eva.submission.repository.CallHomeEventRepository;
import uk.ac.ebi.eva.submission.service.GlobusDirectoryProvisioner;
import uk.ac.ebi.eva.submission.service.GlobusTokenRefreshService;
import uk.ac.ebi.eva.submission.util.SchemaDownloader;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class CallHomeWSIntegrationValidationFailureTest {
    private static String callhomeSchemaURL = "https://dummy_url";

    @Autowired
    private CallHomeEventRepository callHomeEventRepository;

    @Autowired
    private SchemaDownloader schemaDownloader;

    @MockBean
    private GlobusTokenRefreshService globusTokenRefreshService;

    @MockBean
    private GlobusDirectoryProvisioner globusDirectoryProvisioner;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        schemaDownloader.evictLatestTagCache();
        schemaDownloader.evictSchemaCache();
    }

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        // datasource properties
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("eva.email.server", () -> "test-email-server");
        registry.add("eva.email.port", () -> 1025);
        registry.add("eva.helpdesk.email", () -> "test-helpdesk-email");
        registry.add("callhome.schema.url", () -> callhomeSchemaURL);
    }

    @Test
    @Transactional
    public void testRegisterCallHomeEvent_InternalServerError() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode callHomeJsonRootNode = getCallHomeJson(mapper);

        mvc.perform(post("/v1/call-home/events")
                        .content(mapper.writeValueAsString(callHomeJsonRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Could not register event as an exception occurred"));

        Iterable<CallHomeEventEntity> iterable = callHomeEventRepository.findAll();
        List<CallHomeEventEntity> callHomeEventEntityList = StreamSupport
                .stream(iterable.spliterator(), false)
                .collect(Collectors.toList());

        assertThat(callHomeEventEntityList.size()).isEqualTo(0);
    }

    private ObjectNode getCallHomeJson(ObjectMapper mapper) {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("deploymentId", "8f5bb4ea-9fc4-4117-91c6-9966d124e876");
        rootNode.put("runId", "8f5bb4ea-9fc4-4117-91c6-9966d124e876");
        rootNode.put("eventType", "VALIDATION_COMPLETED");
        rootNode.put("cliVersion", "test-cli-version");
        rootNode.put("createdAt", "2020-01-01T00:00:00Z");
        rootNode.put("runtimeSeconds", 123);
        rootNode.put("executor", "native");
        rootNode.putArray("tasks").add("validate").add("submit");

        return rootNode;
    }
}
