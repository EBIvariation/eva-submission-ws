package uk.ac.ebi.eva.submission.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = SubmissionWSIntegrationTest.DockerPostgreDataSourceInitializer.class)
public class SubmissionWSIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private SubmissionRepository submissionRepository;

    @Container
    public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:9.6");

    public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgreDBContainer.getUsername(),
                    "spring.datasource.password=" + postgreDBContainer.getPassword()
            );
        }
    }

    @Test
    @Transactional
    public void testCreateDirectory() throws Exception {
        HttpHeaders httpHeaders = new HttpHeaders();

        String response = mvc.perform(post("/v1/ftp")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SubmissionStatus submissionStatus = submissionRepository.findBySubmissionId(response);
        assertThat(submissionStatus).isNotNull();
        assertThat(submissionStatus.getSubmissionId()).isEqualTo(response);
        assertThat(submissionStatus.isCompleted()).isEqualTo(false);
        assertThat(submissionStatus).isNotNull();

        mvc.perform(put("/v1/completed")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(response))
                .andExpect(status().isOk());

        SubmissionStatus submissionStatus1 = submissionRepository.findBySubmissionId(response);
        assertThat(submissionStatus1).isNotNull();
        assertThat(submissionStatus1.getSubmissionId()).isEqualTo(response);
        assertThat(submissionStatus.isCompleted()).isEqualTo(true);
    }

}