package uk.ac.ebi.eva.submission.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

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
import uk.ac.ebi.eva.submission.model.SubmissionUser;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionUserRepository;
import uk.ac.ebi.eva.submission.service.GlobusDirectoryProvisioner;
import uk.ac.ebi.eva.submission.service.GlobusTokenRefreshService;
import uk.ac.ebi.eva.submission.service.LoginMethod;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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

    @Autowired
    private SubmissionUserRepository submissionUserRepository;

    @MockBean
    private WebinTokenService webinTokenService;

    @MockBean
    private LsriTokenService lsriTokenService;

    @MockBean
    private GlobusTokenRefreshService globusTokenRefreshService;

    @MockBean
    private GlobusDirectoryProvisioner globusDirectoryProvisioner;

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
    public void testSubmissionAuthenticateLsri() throws Exception {
        String userId = "lsriuser@lsri.com";
        String token = "lsriUserToken";
        String deviceCode = "deviceCode";
        String expiresIn = "600";
        when(lsriTokenService.pollForToken(anyString(), anyInt())).thenReturn(token);

        HttpHeaders httpHeaders = new HttpHeaders();
        String userToken = mvc.perform(post("/v1/submission/auth/lsri")
                        .headers(httpHeaders)
                        .param("deviceCode", deviceCode)
                        .param("expiresIn", expiresIn)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(userToken).isNotNull();
        assertThat(userToken).isEqualTo(token);
    }


    @Test
    @Transactional
    public void testSubmissionInitiate() throws Exception {
        String userToken = "webinUserToken";
        SubmissionUser user = getWebinUser();
        when(webinTokenService.getWebinUserFromToken(anyString())).thenReturn(user);
        doNothing().when(globusTokenRefreshService).refreshToken();
        doNothing().when(globusDirectoryProvisioner).createSubmissionDirectory(anyString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        String submissionId = new ObjectMapper().readTree(mvc.perform(post("/v1/submission/initiate")
                                                                              .headers(httpHeaders)
                                                                              .contentType(MediaType.APPLICATION_JSON))
                                                             .andExpect(status().isOk())
                                                             .andReturn().getResponse().getContentAsString())
                                                .get("submissionId").asText();

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        assertThat(submission).isNotNull();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.OPEN.toString());
        assertThat(submission.getInitiationTime()).isNotNull();
        assertThat(submission.getUploadedTime()).isNull();
        assertThat(submission.getCompletionTime()).isNull();

        SubmissionUser submissionUser= submission.getUser();
        assertThat(submissionUser.getUserId()).isEqualTo(user.getUserId());
        assertThat(submissionUser.getLoginType()).isEqualTo(user.getLoginType());
        assertThat(submissionUser.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(submissionUser.getLastName()).isEqualTo(user.getLastName());
        assertThat(submissionUser.getEmailId()).isEqualTo(user.getEmailId());
    }


    @Test
    @Transactional
    public void testSubmissionGetStatus() throws Exception {
        String userToken = "webinUserToken";
        SubmissionUser user = getWebinUser();
        when(webinTokenService.getWebinUserFromToken(anyString())).thenReturn(user);

        String submissionId = createNewSubmissionEntry(user);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(get("/v1/submission/" + submissionId + "/status")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(status().isOk(), content().string(SubmissionStatus.OPEN.toString()));
    }

    @Test
    @Transactional
    public void testMarkSubmissionUploaded() throws Exception {
        String userToken = "webinUserToken";
        SubmissionUser user = getWebinUser();
        when(webinTokenService.getWebinUserFromToken(anyString())).thenReturn(user);

        String submissionId = createNewSubmissionEntry(user);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
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
        String userToken = "webinUserToken";
        SubmissionUser user = getWebinUser();
        when(webinTokenService.getWebinUserFromToken(anyString())).thenReturn(user);

        String submissionId = createNewSubmissionEntry(user);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);

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
        String userToken = "webinUserToken";
        SubmissionUser user = getWebinUser();
        when(webinTokenService.getWebinUserFromToken(anyString())).thenReturn(user);

        String submissionId = createNewSubmissionEntry(user);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/status/complete")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private SubmissionUser getWebinUser(){
        String userId = "webinUserId";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String firstName = "webin_first_name";
        String lastName = "webin_last_name";
        String email = "webinUserId@webin.com";

        return new SubmissionUser(userId, loginType, firstName, lastName, email);
    }

    private String createNewSubmissionEntry(SubmissionUser submissionUser) {
        submissionUserRepository.save(submissionUser);

        String submissionId = UUID.randomUUID().toString();
        Submission submission = new Submission(submissionId);
        submission.setUser(submissionUser);
        submission.setStatus(SubmissionStatus.OPEN.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submissionRepository.save(submission);

        return submissionId;

    }
}
