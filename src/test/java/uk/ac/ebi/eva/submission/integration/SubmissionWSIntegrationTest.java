package uk.ac.ebi.eva.submission.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.eva.submission.model.Submission;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SubmissionWSIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Value("${eva.submission.dropbox}")
    private String submissionDropbox;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6");

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Test
    @Transactional
    public void testSubmissionInitiate() throws Exception {
        HttpHeaders httpHeaders = new HttpHeaders();

        String submissionId = mvc.perform(post("/v1/submission/initiate")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        assertThat(submission).isNotNull();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.OPEN.toString());
        assertThat(submission.getInitiationTime()).isNotNull();
        assertThat(submission.getUploadedTime()).isNull();
        assertThat(submission.getCompletionTime()).isNull();
        assertThat(Files.exists(Paths.get(submissionDropbox + "/" + submissionId))).isTrue();
        assertThat(Files.isDirectory(Paths.get(submissionDropbox + "/" + submissionId))).isTrue();

        // Cleanup - delete directory
        new File(submissionDropbox + "/" + submissionId).deleteOnExit();
    }

    @Test
    @Transactional
    public void testSubmissionGetStatus() throws Exception {
        String submissionId = createNewSubmissionEntry();

        HttpHeaders httpHeaders = new HttpHeaders();
        mvc.perform(get("/v1/submission/" + submissionId + "/status")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(status().isOk(), content().string(SubmissionStatus.OPEN.toString()));
    }

    @Test
    @Transactional
    public void testMarkSubmissionUploaded() throws Exception {
        String submissionId = createNewSubmissionEntry();

        HttpHeaders httpHeaders = new HttpHeaders();
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        assertThat(submission).isNotNull();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.UPLOADED.toString());
        assertThat(submission.getUploadedTime()).isNotNull();
        assertThat(submission.getCompletionTime()).isNull();
    }

    @Test
    @Transactional
    public void testMarkSubmissionStatusCorrect() throws Exception {
        String submissionId = createNewSubmissionEntry();

        HttpHeaders httpHeaders = new HttpHeaders();
        mvc.perform(put("/v1/submission/" + submissionId + "/status/COMPLETED")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        assertThat(submission).isNotNull();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.COMPLETED.toString());
        assertThat(submission.getCompletionTime()).isNotNull();
    }

    @Test
    @Transactional
    public void testMarkSubmissionStatusWrong() throws Exception {
        String submissionId = createNewSubmissionEntry();

        HttpHeaders httpHeaders = new HttpHeaders();
        mvc.perform(put("/v1/submission/" + submissionId + "/status/complete")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private String createNewSubmissionEntry() {
        String submissionId = UUID.randomUUID().toString();
        Submission submission = new Submission(submissionId);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submissionRepository.save(submission);

        return submissionId;

    }

}