package uk.ac.ebi.eva.submission.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.entity.SubmissionDetails;
import uk.ac.ebi.eva.submission.entity.SubmissionProcessing;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStatus;
import uk.ac.ebi.eva.submission.model.SubmissionProcessingStep;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.repository.SubmissionAccountRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionDetailsRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionProcessingRepository;
import uk.ac.ebi.eva.submission.repository.SubmissionRepository;
import uk.ac.ebi.eva.submission.service.GlobusDirectoryProvisioner;
import uk.ac.ebi.eva.submission.service.GlobusTokenRefreshService;
import uk.ac.ebi.eva.submission.service.LoginMethod;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;
import uk.ac.ebi.eva.submission.util.MailSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SubmissionWSIntegrationTest {

    @Value("${controller.auth.admin.username}")
    private String TEST_ADMIN_USERNAME;

    @Value("${controller.auth.admin.password}")
    private String TEST_ADMIN_PASSWORD;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionAccountRepository submissionAccountRepository;

    @Autowired
    private SubmissionDetailsRepository submissionDetailsRepository;

    @Autowired
    private SubmissionProcessingRepository submissionProcessingRepository;

    @MockBean
    private WebinTokenService webinTokenService;

    @MockBean
    private LsriTokenService lsriTokenService;

    @MockBean
    private GlobusTokenRefreshService globusTokenRefreshService;

    @MockBean
    private GlobusDirectoryProvisioner globusDirectoryProvisioner;

    @MockBean
    private MailSender mailSender;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6")
            .withInitScript("init.sql");

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
        SubmissionAccount webinUserAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(webinUserAccount);
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

        SubmissionAccount submissionAccount = submission.getSubmissionAccount();
        assertThat(submissionAccount.getUserId()).isEqualTo(webinUserAccount.getUserId());
        assertThat(submissionAccount.getLoginType()).isEqualTo(webinUserAccount.getLoginType());
        assertThat(submissionAccount.getFirstName()).isEqualTo(webinUserAccount.getFirstName());
        assertThat(submissionAccount.getLastName()).isEqualTo(webinUserAccount.getLastName());
        assertThat(submissionAccount.getPrimaryEmail()).isEqualTo(webinUserAccount.getPrimaryEmail());
        assertThat(new HashSet<>(submissionAccount.getSecondaryEmails())).isEqualTo(new HashSet<>(webinUserAccount.getSecondaryEmails()));

    }

    @Test
    @Transactional
    public void testUserUpdate() throws Exception {
        String userToken = "webinUserToken";
        doNothing().when(globusTokenRefreshService).refreshToken();
        doNothing().when(globusDirectoryProvisioner).createSubmissionDirectory(anyString());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);

        SubmissionAccount orgUserAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(orgUserAccount);

        // create user
        new ObjectMapper().readTree(mvc.perform(post("/v1/submission/initiate")
                                .headers(httpHeaders)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("submissionId").asText();

        SubmissionAccount userAccountInDB = submissionAccountRepository.findById(orgUserAccount.getId()).get();
        assertThat(userAccountInDB.getId()).isEqualTo(orgUserAccount.getId());
        assertThat(userAccountInDB.getUserId()).isEqualTo(orgUserAccount.getUserId());
        assertThat(userAccountInDB.getLoginType()).isEqualTo(orgUserAccount.getLoginType());
        assertThat(userAccountInDB.getPrimaryEmail()).isEqualTo(orgUserAccount.getPrimaryEmail());
        assertThat(userAccountInDB.getFirstName()).isEqualTo(orgUserAccount.getFirstName());
        assertThat(userAccountInDB.getLastName()).isEqualTo(orgUserAccount.getLastName());

        // update user's primary email, first name and last name
        SubmissionAccount otherUserAccount = new SubmissionAccount(orgUserAccount.getUserId(), orgUserAccount.getLoginType());
        otherUserAccount.setPrimaryEmail("other_primary_email");
        otherUserAccount.setFirstName("other_first_name");
        otherUserAccount.setLastName("other_last_name");
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(otherUserAccount);

        // user with same id is already present in db, but it's primary email will not match and should be updated in db
        new ObjectMapper().readTree(mvc.perform(post("/v1/submission/initiate")
                                .headers(httpHeaders)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get("submissionId").asText();


        userAccountInDB = submissionAccountRepository.findById(otherUserAccount.getId()).get();
        // assert id, user id and login type remains same for the user
        assertThat(userAccountInDB.getId()).isEqualTo(orgUserAccount.getId());
        assertThat(userAccountInDB.getUserId()).isEqualTo(orgUserAccount.getUserId());
        assertThat(userAccountInDB.getLoginType()).isEqualTo(orgUserAccount.getLoginType());

        // assert primary email, first name and last name is updated
        assertThat(userAccountInDB.getPrimaryEmail()).isEqualTo(otherUserAccount.getPrimaryEmail());
        assertThat(userAccountInDB.getFirstName()).isEqualTo(otherUserAccount.getFirstName());
        assertThat(userAccountInDB.getLastName()).isEqualTo(otherUserAccount.getLastName());
    }


    @Test
    @Transactional
    public void testSubmissionGetStatus() throws Exception {
        String userToken = "webinUserToken";
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId = createNewSubmissionEntry(submissionAccount);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(get("/v1/submission/" + submissionId + "/status")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(status().isOk(), content().string(SubmissionStatus.OPEN.toString()));
    }

    @Test
    @Transactional
    public void testSubmissionGetStatusSubmissionDoesNotExist() throws Exception {
        String userToken = "webinUserToken";
        String submissionId = "test123";
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(get("/v1/submission/" + submissionId + "/status")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(status().isNotFound(), content().string("Submission with Id test123 does not exist"));
    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploadedd() throws Exception {
        String userToken = "webinUserToken";
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);
        doNothing().when(mailSender).sendEmail(anyString(), anyString(), anyString());

        String submissionId = createNewSubmissionEntry(submissionAccount);
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        String metadataJson = "{\"project\": {\"title\":\"" + projectTitle + "\",\"description\":\"" + projectDescription + "\"}}";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(metadataJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Submission submission = submissionRepository.findBySubmissionId(submissionId);
        assertThat(submission).isNotNull();
        assertThat(submission.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.UPLOADED.toString());
        assertThat(submission.getUploadedTime()).isNotNull();
        assertThat(submission.getCompletionTime()).isNull();

        SubmissionDetails submissionDetails = submissionDetailsRepository.findBySubmissionId(submissionId);
        assertThat(submissionDetails).isNotNull();
        assertThat(submissionDetails.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submissionDetails.getProjectTitle()).isEqualTo(projectTitle);
        assertThat(submissionDetails.getProjectDescription()).isEqualTo(projectDescription);
        assertThat(submissionDetails.getMetadataJson()).isNotNull();
        assertThat(submissionDetails.getMetadataJson().get("project").get("title").asText()).isEqualTo(projectTitle);
        assertThat(submissionDetails.getMetadataJson().get("project").get("title").asText()).isEqualTo(projectTitle);
    }

    @Test
    @Transactional
    public void testRequiredMetadataFieldsNotProvided() throws Exception {
        String userToken = "webinUserToken";
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);
        doNothing().when(mailSender).sendEmail(anyString(), anyString(), anyString());

        String submissionId = createNewSubmissionEntry(submissionAccount);
        String projectDescription = "test_project_description";
        String metadataJson = "{\"project\": {\"description\":\"" + projectDescription + "\"}}";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);

        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(metadataJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Required fields project title and project description " +
                        "could not be found in metadata json"));
    }

    @Test
    @Transactional
    public void testSubmissionDoesNotExistException() throws Exception {
        String userToken = "webinUserToken";
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId = "wrong_submission_id";
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        String metadataJson = "{\"project\": {\"title\":\"" + projectTitle + "\",\"description\":\"" + projectDescription + "\"}}";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(metadataJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Given submission with id " + submissionId + " does not exist"));
    }

    @Test
    @Transactional
    public void testMarkSubmissionStatusCorrect() throws Exception {
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId = createNewSubmissionEntry(submissionAccount);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);

        mvc.perform(put("/v1/admin/submission/" + submissionId + "/status/COMPLETED")
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
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId = createNewSubmissionEntry(submissionAccount);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission/" + submissionId + "/status/complete")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testGetSubmissionByStatus() throws Exception {
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId1 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.UPLOADED);
        String submissionId2 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.UPLOADED);
        String submissionId3 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.OPEN);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(get("/v1/admin/submissions/status/" + SubmissionStatus.UPLOADED)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].submissionId").value(containsInAnyOrder(submissionId1, submissionId2)))
                .andExpect(jsonPath("$[*].submissionId").value(not(containsInAnyOrder(submissionId3))));
    }


    @Test
    @Transactional
    public void testGetSubmissionProcessingByStepAndStatus() throws Exception {
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId1 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId1, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.READY_FOR_PROCESSING);
        String submissionId2 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId2, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.ON_HOLD);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(get("/v1/admin/submission-processes/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.READY_FOR_PROCESSING)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].submissionId").value(containsInAnyOrder(submissionId1)));
    }


    @Test
    @Transactional
    public void testMarkSubmissionProcessingByStatus() throws Exception {
        SubmissionAccount submissionAccount = getWebinUserAccount();
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(submissionAccount);

        String submissionId1 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId1, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.READY_FOR_PROCESSING);
        String submissionId2 = createNewSubmissionEntry(submissionAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId2, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.ON_HOLD);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission-process/" + submissionId1 + "/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.SUCCESS)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId1);
        assertThat(submissionProc).isNotNull();
        assertThat(submissionProc.getSubmissionId()).isEqualTo(submissionId1);
        assertThat(submissionProc.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(submissionProc.getStatus()).isEqualTo(SubmissionProcessingStatus.SUCCESS.toString());
        assertThat(submissionProc.getLastUpdateTime()).isNotNull();
    }




    private SubmissionAccount getWebinUserAccount() {
        String accountId = "webinAccountId";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String firstName = "webin_first_name";
        String lastName = "webin_last_name";
        String primaryEmail = "webinUserId@webin.com";
        List<String> secondaryEmails = new ArrayList<>();
        secondaryEmails.add("webinUserId_1@webin.com");
        secondaryEmails.add("webinUserId_2@webin.com");
        return new SubmissionAccount(accountId, loginType, firstName, lastName, primaryEmail, secondaryEmails);
    }

    private String createNewSubmissionEntry(SubmissionAccount submissionAccount) {
        return createNewSubmissionEntry(submissionAccount, SubmissionStatus.OPEN);
    }

    private String createNewSubmissionEntry(SubmissionAccount submissionAccount, SubmissionStatus status) {
        submissionAccountRepository.save(submissionAccount);

        String submissionId = UUID.randomUUID().toString();
        Submission submission = new Submission(submissionId);
        submission.setSubmissionAccount(submissionAccount);
        submission.setStatus(status.toString());
        submission.setInitiationTime(LocalDateTime.now());
        submissionRepository.save(submission);

        return submissionId;
    }

    private void createNewSubmissionProcessingEntry(String submissionId, SubmissionProcessingStep step,
                                                    SubmissionProcessingStatus status) {
        SubmissionProcessing submissionProcessing = new SubmissionProcessing(submissionId);
        submissionProcessing.setStep(step.toString());
        submissionProcessing.setStatus(status.toString());
        submissionProcessing.setPriority(5);
        submissionProcessing.setLastUpdateTime(LocalDateTime.now());
        submissionProcessingRepository.save(submissionProcessing);
    }
}
