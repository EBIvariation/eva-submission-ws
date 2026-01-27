package uk.ac.ebi.eva.submission.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class CallHomeWSIntegrationTest {
    @Autowired
    private CallHomeEventRepository callHomeEventRepository;

    @MockBean
    private GlobusTokenRefreshService globusTokenRefreshService;

    @MockBean
    private GlobusDirectoryProvisioner globusDirectoryProvisioner;

    @Autowired
    private MockMvc mvc;


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
    }


    @Test
    @Transactional
    public void testRegisterCallHomeEvent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode callHomeJsonRootNode = getCallHomeJson(mapper);

        mvc.perform(post("/v1/call-home/events")
                        .content(mapper.writeValueAsString(callHomeJsonRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Iterable<CallHomeEventEntity> iterable = callHomeEventRepository.findAll();
        List<CallHomeEventEntity> callHomeEventEntityList = StreamSupport
                .stream(iterable.spliterator(), false)
                .collect(Collectors.toList());

        assertThat(callHomeEventEntityList.size()).isEqualTo(1);

        CallHomeEventEntity callHomeEventEntity = callHomeEventEntityList.get(0);
        assertThat(callHomeEventEntity.getDeploymentId()).isEqualTo("test-deployment-id");
        assertThat(callHomeEventEntity.getRunId()).isEqualTo("test-run-id");
        assertThat(callHomeEventEntity.getEventType()).isEqualTo("test-event-type");
        assertThat(callHomeEventEntity.getCliVersion()).isEqualTo("test-cli-version");
        assertThat(callHomeEventEntity.getCreatedAt()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(callHomeEventEntity.getRuntimeSeconds()).isEqualTo(123);
        assertThat(callHomeEventEntity.getExecutor()).isEqualTo("Native");
        assertThat(callHomeEventEntity.getTasks()).isEqualTo("VALIDATION,SUBMIT");
        assertThat(callHomeEventEntity.getTrimDown()).isEqualTo(Boolean.FALSE);
        assertThat(callHomeEventEntity.getUserName()).isEqualTo("test-user-name");

        assertThat(callHomeEventEntity.getRawPayload().toString()).isEqualTo(callHomeJsonRootNode.toString());
    }

    @Test
    @Transactional
    public void testRegisterCallHomeEvent_someFieldsAreNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode callHomeJsonRootNode = getCallHomeJson(mapper);
        callHomeJsonRootNode.putNull("eventType");
        callHomeJsonRootNode.putNull("userName");

        mvc.perform(post("/v1/call-home/events")
                        .content(mapper.writeValueAsString(callHomeJsonRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Iterable<CallHomeEventEntity> iterable = callHomeEventRepository.findAll();
        List<CallHomeEventEntity> callHomeEventEntityList = StreamSupport
                .stream(iterable.spliterator(), false)
                .collect(Collectors.toList());

        assertThat(callHomeEventEntityList.size()).isEqualTo(1);

        CallHomeEventEntity callHomeEventEntity = callHomeEventEntityList.get(0);
        assertThat(callHomeEventEntity.getDeploymentId()).isEqualTo("test-deployment-id");
        assertThat(callHomeEventEntity.getRunId()).isEqualTo("test-run-id");
        assertThat(callHomeEventEntity.getEventType()).isNull();
        assertThat(callHomeEventEntity.getCliVersion()).isEqualTo("test-cli-version");
        assertThat(callHomeEventEntity.getCreatedAt()).isEqualTo(LocalDateTime.parse("2020-01-01T00:00:00"));
        assertThat(callHomeEventEntity.getRuntimeSeconds()).isEqualTo(123);
        assertThat(callHomeEventEntity.getExecutor()).isEqualTo("Native");
        assertThat(callHomeEventEntity.getTasks()).isEqualTo("VALIDATION,SUBMIT");
        assertThat(callHomeEventEntity.getTrimDown()).isEqualTo(Boolean.FALSE);
        assertThat(callHomeEventEntity.getUserName()).isNull();

        assertThat(callHomeEventEntity.getRawPayload().toString()).isEqualTo(callHomeJsonRootNode.toString());
    }

    private ObjectNode getCallHomeJson(ObjectMapper mapper) {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("deploymentId", "test-deployment-id");
        rootNode.put("runId", "test-run-id");
        rootNode.put("eventType", "test-event-type");
        rootNode.put("cliVersion", "test-cli-version");
        rootNode.put("createdAt", "2020-01-01T00:00:00");
        rootNode.put("runtimeSeconds", 123);
        rootNode.put("executor", "Native");
        rootNode.put("tasks", "VALIDATION,SUBMIT");
        rootNode.put("trimDown", "false");
        rootNode.put("userName", "test-user-name");


        return rootNode;
    }

}
