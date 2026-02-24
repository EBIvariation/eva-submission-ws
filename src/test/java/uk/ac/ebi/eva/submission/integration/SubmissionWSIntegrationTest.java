package uk.ac.ebi.eva.submission.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
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
import uk.ac.ebi.eva.submission.util.EnaDownloader;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SubmissionWSIntegrationTest {

    @Value("${controller.auth.admin.username}")
    private String TEST_ADMIN_USERNAME;

    @Value("${controller.auth.admin.password}")
    private String TEST_ADMIN_PASSWORD;

    @Autowired
    private EntityManager entityManager;

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
    private EnaDownloader enaDownloader;

    @MockBean
    private WebinTokenService webinTokenService;

    @MockBean
    private LsriTokenService lsriTokenService;

    @MockBean
    private GlobusTokenRefreshService globusTokenRefreshService;

    @MockBean
    private GlobusDirectoryProvisioner globusDirectoryProvisioner;

    private static String evaHelpdeskEmail = "test_eva_helpdesk@email.com";
    private static String webinUserPrimaryEmail = "webinUserId@webin.com";
    private static List<String> webinUserSecondaryEmails = Arrays.asList("webinUserId_1@webin.com", "webinUserId_2@webin.com");
    private String userToken = "webinUserToken";
    private SubmissionAccount webinUserAccount = getWebinUserAccount();
    private String submissionId;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6")
            .withInitScript("init.sql");
    @Container
    static GenericContainer<?> mailhog = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog"))
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        // datasource properties
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // MailHog Configuration
        registry.add("eva.email.server", mailhog::getHost);
        registry.add("eva.email.port", () -> mailhog.getMappedPort(1025));
        registry.add("eva.helpdesk.email", () -> evaHelpdeskEmail);
        registry.add("callhome.schema.url", () -> "https://dummy_url");
    }

    @BeforeEach
    public void setup() {
        doNothing().when(globusTokenRefreshService).refreshToken();
        doNothing().when(globusDirectoryProvisioner).createSubmissionDirectory(anyString());
        when(webinTokenService.getWebinUserAccountFromToken(anyString())).thenReturn(webinUserAccount);

        submissionId = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.OPEN);
    }

    @Test
    @Transactional
    public void testSubmissionAuthenticateLsri() throws Exception {
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
    public void testSubmissionInitiateWithNullUserName() throws Exception {
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
        SubmissionAccount orgUserAccount = webinUserAccount;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);

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
        SubmissionAccount otherUserAccount = new SubmissionAccount(orgUserAccount.getUserId(), orgUserAccount.getLoginType(),
                "other_first_name", "other_last_name", "other_primary_email");
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
        String submissionId = "test123";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(get("/v1/submission/" + submissionId + "/status")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(status().isNotFound(), content().string("Submission with id " + submissionId + " does not exist"));
    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploaded_ContainsNonDeprecatedVersion() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        int taxonomyId = 9606;

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);
        projectNode.put("taxId", taxonomyId);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        analysisNode1.put("evidenceType", "allele_frequency");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("evidenceType", "allele_frequency");
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);
        metadataRootNode.put("$schema", "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/refs/tags/v0.5.1/eva_sub_cli/etc/eva_schema.json");

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertThat(submissionDetails.getMetadataJson().get("project").get("description").asText()).isEqualTo(projectDescription);

        // assert email sent to user and helpdesk
        assertEmailsSentToUserAndHelpDesk(false, false);

    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploaded_ContainsDeprecatedVersion() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        int taxonomyId = 9606;

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);
        projectNode.put("taxId", taxonomyId);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        analysisNode1.put("evidenceType", "allele_frequency");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("evidenceType", "allele_frequency");
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);
        metadataRootNode.put("$schema", "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/refs/tags/v0.3.9/eva_sub_cli/etc/eva_schema.json");

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertThat(submissionDetails.getMetadataJson().get("project").get("description").asText()).isEqualTo(projectDescription);

        // assert email sent to user and helpdesk
        assertEmailsSentToUserAndHelpDesk(false, true);

    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploaded_ConsentStatementRequired_ContainsEvidenceTypeGenotype() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        int taxonomyId = 9606;

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);
        projectNode.put("taxId", taxonomyId);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        analysisNode1.put("evidenceType", "genotype");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("evidenceType", "allele_frequency");
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertThat(submissionDetails.getMetadataJson().get("project").get("description").asText()).isEqualTo(projectDescription);

        // assert email sent to user and helpdesk
        assertEmailsSentToUserAndHelpDesk(true, true);

    }


    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploaded_ConsentStatementNotRequired_ContainsEvidenceTypeAlleleFrequency() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        int taxonomyId = 9606;

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);
        projectNode.put("taxId", taxonomyId);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        analysisNode1.put("evidenceType", "allele_frequency");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("evidenceType", "allele_frequency");
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertThat(submissionDetails.getMetadataJson().get("project").get("description").asText()).isEqualTo(projectDescription);

        // assert email sent to user and helpdesk
        assertEmailsSentToUserAndHelpDesk(false, true);

    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploadedForLargeProjectTitleAndDescription_ConsentStatementRequired_NoEvidenceTypeProvided() throws Exception {
        String projectTitle = buildLargeStringOfLength(600);
        String projectDescription = buildLargeStringOfLength(5500);

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);
        projectNode.put("taxId", 9606);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        assertEquals(metadataRootNode.get("project").get("title").asText().length(), 600);
        assertEquals(metadataRootNode.get("project").get("description").asText().length(), 5500);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertEquals(500, submissionDetails.getProjectTitle().length());
        assertEquals(5000, submissionDetails.getProjectDescription().length());
        assertThat(submissionDetails.getMetadataJson()).isNotNull();
        assertEquals(600, submissionDetails.getMetadataJson().get("project").get("title").asText().length());
        assertEquals(5500, submissionDetails.getMetadataJson().get("project").get("description").asText().length());

        // assert email sent to User and Helpdesk
        assertEmailsSentToUserAndHelpDesk(true, true);
    }

    private String buildLargeStringOfLength(int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> "A")
                .collect(Collectors.joining());
    }

    @Test
    @Transactional
    public void testUploadMetadataJsonAndMarkUploaded_RetrieveProjectDetailsWithProjectAccession() throws Exception {
        String projectAccession = "PRJEB12345";
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        String taxonomyId = "9606";

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("projectAccession", projectAccession);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        ArrayNode analysisArrayNode = mapper.createArrayNode();
        ObjectNode analysisNode1 = mapper.createObjectNode();
        analysisNode1.put("analysisAlias", "A1");
        analysisNode1.put("evidenceType", "allele_frequency");
        ObjectNode analysisNode2 = mapper.createObjectNode();
        analysisNode2.put("evidenceType", "allele_frequency");
        analysisNode2.put("analysisAlias", "A2");

        analysisArrayNode.add(analysisNode1);
        analysisArrayNode.add(analysisNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);
        metadataRootNode.put("analysis", analysisArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xmlString = "<PROJECT_SET>" +
                "<PROJECT>" +
                "<TITLE>" + projectTitle + "</TITLE>" +
                "<DESCRIPTION>" + projectDescription + "</DESCRIPTION>" +
                "<SUBMISSION_PROJECT>" +
                "<ORGANISM><TAXON_ID>" + taxonomyId + "</TAXON_ID></ORGANISM>" +
                "</SUBMISSION_PROJECT>" +
                "</PROJECT>" +
                "</PROJECT_SET>";
        Document xmlDoc = builder.parse(new InputSource(new StringReader(xmlString)));

        when(enaDownloader.downloadXmlFromEna(projectAccession)).thenReturn(xmlDoc);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
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
        assertThat(submissionDetails.getMetadataJson().get("project").get("projectAccession").asText()).isEqualTo(projectAccession);

        // assert email sent to user and helpdesk
        assertEmailsSentToUserAndHelpDesk(false, true);

    }

    @Test
    @Transactional
    public void testMarkSubmissionUploadNoFileInfoInMetadatajson() throws Exception {
        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn("");

        // create metadata json
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metadataRootNode = mapper.createObjectNode();
        ArrayNode filesArrayNode = mapper.createArrayNode();
        metadataRootNode.put("files", filesArrayNode);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Metadata json file does not have any file info"));
    }


    @Test
    @Transactional
    public void testMarkSubmissionUploadErrorGettingInfoFromGlobus() throws Exception {
        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn("");

        // create metadata json
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metadataRootNode = mapper.createObjectNode();
        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        filesArrayNode.add(fileNode1);
        metadataRootNode.put("files", filesArrayNode);


        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to retrieve any file info from submission directory."));
    }

    @Test
    @Transactional
    public void testMarkSubmissionUploadFileNotUploaded() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();
        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        filesArrayNode.add(fileNode1);
        metadataRootNode.put("files", filesArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();
        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file10.vcf");
        dataNode1.put("size", 12345L);
        dataNodeArray.add(dataNode1);
        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("There are some files mentioned in metadata json but not uploaded. Files : file1.vcf\n"));
    }

    @Test
    @Transactional
    public void testMarkSubmissionUploadFileSizeMismatch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();
        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        filesArrayNode.add(fileNode1);
        metadataRootNode.put("files", filesArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();
        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12346L);
        dataNodeArray.add(dataNode1);
        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("There are some files mentioned in metadata json whose size does not match with the files uploaded.\n" +
                        "file1.vcf: metadata json file size (12345) is not equal to uploaded file size (12346)\n"));
    }

    @Test
    @Transactional
    public void testMarkSubmissionUploadFileNotUploadedAndFileSizeMismatch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();
        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf");
        fileNode2.put("fileSize", 67890L);
        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);
        metadataRootNode.put("files", filesArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();
        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12346L);
        dataNodeArray.add(dataNode1);
        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("There are some files mentioned in metadata json but not uploaded. Files : file2.vcf\n" +
                        "There are some files mentioned in metadata json whose size does not match with the files uploaded.\n" +
                        "file1.vcf: metadata json file size (12345) is not equal to uploaded file size (12346)\n"));
    }

    @Test
    @Transactional
    public void testRequiredMetadataFieldsNotProvided() throws Exception {
        String projectTitle = "test_project_title";

        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        filesArrayNode.add(fileNode1);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);

        // create globus list directory json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);

        dataNodeArray.add(dataNode1);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Some of the required parameters are missing from the metadata. " +
                        "Missing parameters: [project description, project taxonomy]"));
    }


    @Test
    @Transactional
    public void testRequiredMetadataFieldsNotProvided_CouldNotRetrieveProjectDetailsWithProjectAccession() throws Exception {
        String projectAccession = "PRJEB12345";
        ObjectMapper mapper = new ObjectMapper();

        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("projectAccession", projectAccession);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        filesArrayNode.add(fileNode1);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);

        // create globus list directory json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);

        dataNodeArray.add(dataNode1);

        globusRootNode.put("DATA", dataNodeArray);

        when(globusDirectoryProvisioner.listSubmittedFiles(webinUserAccount.getId() + "/" + submissionId)).thenReturn(mapper.writeValueAsString(globusRootNode));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xmlString = "<PROJECT_SET> <PROJECT> </PROJECT> </PROJECT_SET>";
        Document xmlDoc = builder.parse(new InputSource(new StringReader(xmlString)));

        when(enaDownloader.downloadXmlFromEna(projectAccession)).thenReturn(xmlDoc);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(mapper.writeValueAsString(metadataRootNode))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Could not retrieve some of the required parameters from ENA " +
                        "for the project " + projectAccession + ". " +
                        "Missing parameters: [project title, project description, project taxonomy]"));
    }


    @Test
    @Transactional
    public void testSubmissionDoesNotExistException() throws Exception {
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
                .andExpect(content().string("Submission with id " + submissionId + " does not exist"));
    }

    @Test
    @Transactional
    public void testSubmissionAlreadyUploaded() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";
        String submissionId = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.UPLOADED);
        String metadataJson = "{\"project\": {\"title\":\"" + projectTitle + "\",\"description\":\"" + projectDescription + "\"}}";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(userToken);
        mvc.perform(put("/v1/submission/" + submissionId + "/uploaded")
                        .headers(httpHeaders)
                        .content(metadataJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Submission " + submissionId + " is not in status OPEN. " +
                        "It cannot be marked as UPLOADED. Current Status: UPLOADED"));
    }

    @Test
    @Transactional
    public void testMarkSubmissionStatusCorrect() throws Exception {
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
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission/" + submissionId + "/status/complete")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testMarkSubmissionStatusSubmissionDoesNotExist() throws Exception {
        String submissionId = "test-wrong-submission-id";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission/" + submissionId + "/status/COMPLETED")
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Submission with id " + submissionId + " does not exist"));
    }

    @Test
    @Transactional
    public void testGetSubmissionByStatus() throws Exception {
        String submissionId1 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.UPLOADED);
        String submissionId2 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.UPLOADED);
        String submissionId3 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.OPEN);

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
        String submissionId1 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId1, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.READY_FOR_PROCESSING);
        String submissionId2 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);
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
    public void testMarkSubmissionProcessStepAndStatus() throws Exception {
        String submissionId1 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);
        createNewSubmissionProcessingEntry(submissionId1, SubmissionProcessingStep.VALIDATION,
                SubmissionProcessingStatus.READY_FOR_PROCESSING);
        String submissionId2 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);
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

    @Test
    @Transactional
    public void testMarkSubmissionProcessStepAndStatusCreateFirstEntry() throws Exception {
        String submissionId = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission-process/" + submissionId + "/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.SUCCESS)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        assertThat(submissionProc).isNotNull();
        assertThat(submissionProc.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submissionProc.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(submissionProc.getStatus()).isEqualTo(SubmissionProcessingStatus.SUCCESS.toString());
        assertThat(submissionProc.getLastUpdateTime()).isNotNull();
    }

    @Test
    @Transactional
    public void testMarkSubmissionProcessStepAndStatusSubmissionDoesNotExist() throws Exception {
        String submissionId = "test-wrong-submission-id";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission-process/" + submissionId + "/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.SUCCESS)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Submission with id " + submissionId + " does not exist"));
    }


    @Test
    @Transactional
    public void testGetSubmissionDetail() throws Exception {
        String projectTitle = "test_project_title";
        String projectDescription = "test_project_description";

        String submissionId1 = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.UPLOADED);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode metadataRootNode = createNewMetadataJSON(mapper, projectTitle, projectDescription);

        createNewSubmissionDetailEntry(submissionId1, projectTitle, projectDescription, metadataRootNode);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(get("/v1/admin/submission/" + submissionId1)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("submissionId").value(submissionId1))
                .andExpect(jsonPath("metadataJson.project.title").value(projectTitle))
                .andExpect(jsonPath("metadataJson.project.description").value(projectDescription));
    }

    @Disabled
    @Test
    @Transactional
    public void testSubmissionProcessingHistory() throws Exception {
        String submissionId = createNewSubmissionEntry(webinUserAccount, SubmissionStatus.PROCESSING);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBasicAuth(TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD);
        mvc.perform(put("/v1/admin/submission-process/" + submissionId + "/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.READY_FOR_PROCESSING)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        SubmissionProcessing submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        assertThat(submissionProc.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submissionProc.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(submissionProc.getStatus()).isEqualTo(SubmissionProcessingStatus.READY_FOR_PROCESSING.toString());

        mvc.perform(put("/v1/admin/submission-process/" + submissionId + "/" + SubmissionProcessingStep.VALIDATION + "/" + SubmissionProcessingStatus.FAILURE)
                        .headers(httpHeaders)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        submissionProc = submissionProcessingRepository.findBySubmissionId(submissionId);
        assertThat(submissionProc.getSubmissionId()).isEqualTo(submissionId);
        assertThat(submissionProc.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(submissionProc.getStatus()).isEqualTo(SubmissionProcessingStatus.FAILURE.toString());

        // Assert Submission Processing History Entry inserted successfully
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = auditReader.getRevisions(SubmissionProcessing.class, submissionId);
        assertThat(revisions).isNotEmpty();

        Number firstRevision = revisions.get(0);
        SubmissionProcessing auditEntryFirstRev = auditReader.find(SubmissionProcessing.class, submissionId, firstRevision);
        assertThat(auditEntryFirstRev).isNotNull();
        assertThat(auditEntryFirstRev.getSubmissionId()).isEqualTo(submissionId);
        assertThat(auditEntryFirstRev.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(auditEntryFirstRev.getStatus()).isEqualTo(SubmissionProcessingStatus.READY_FOR_PROCESSING.toString());

        Number secondRevision = revisions.get(1);
        SubmissionProcessing auditEntrySecondRev = auditReader.find(SubmissionProcessing.class, submissionId, secondRevision);
        assertThat(auditEntrySecondRev).isNotNull();
        assertThat(auditEntrySecondRev.getSubmissionId()).isEqualTo(submissionId);
        assertThat(auditEntrySecondRev.getStep()).isEqualTo(SubmissionProcessingStep.VALIDATION.toString());
        assertThat(auditEntrySecondRev.getStatus()).isEqualTo(SubmissionProcessingStatus.FAILURE.toString());
    }


    private SubmissionAccount getWebinUserAccount() {
        String accountId = "webinAccountId";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String firstName = "webin_first_name";
        String lastName = "webin_last_name";
        return new SubmissionAccount(accountId, loginType, firstName, lastName,
                webinUserPrimaryEmail, webinUserSecondaryEmails);
    }

    private SubmissionAccount getWebinUserAccountWithNullUserName() {
        String accountId = "webinAccountId";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String firstName = null;
        String lastName = null;
        String primaryEmail = webinUserPrimaryEmail;
        return new SubmissionAccount(accountId, loginType, firstName, lastName,
                webinUserPrimaryEmail, webinUserSecondaryEmails);
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
        submissionProcessingRepository.save(submissionProcessing);
    }

    private void createNewSubmissionDetailEntry(String submissionId, String projectTitle, String projectDescription,
                                                JsonNode metadataJson) {
        SubmissionDetails submissionDetail = new SubmissionDetails(submissionId);
        submissionDetail.setProjectTitle(projectTitle);
        submissionDetail.setProjectDescription(projectDescription);
        submissionDetail.setMetadataJson(metadataJson);
        submissionDetailsRepository.save(submissionDetail);
    }

    private JsonNode createNewMetadataJSON(ObjectMapper mapper, String projectTitle, String projectDescription) {
        // create metadata json
        ObjectNode metadataRootNode = mapper.createObjectNode();

        ObjectNode projectNode = mapper.createObjectNode();
        projectNode.put("title", projectTitle);
        projectNode.put("description", projectDescription);

        ArrayNode filesArrayNode = mapper.createArrayNode();
        ObjectNode fileNode1 = mapper.createObjectNode();
        fileNode1.put("fileName", "file1.vcf");
        fileNode1.put("fileSize", 12345L);
        ObjectNode fileNode2 = mapper.createObjectNode();
        fileNode2.put("fileName", "file2.vcf.gz");
        fileNode2.put("fileSize", 67890L);

        filesArrayNode.add(fileNode1);
        filesArrayNode.add(fileNode2);

        metadataRootNode.put("project", projectNode);
        metadataRootNode.put("files", filesArrayNode);

        // create Globus list directory result json
        ObjectNode globusRootNode = mapper.createObjectNode();

        ArrayNode dataNodeArray = mapper.createArrayNode();
        ObjectNode dataNode1 = mapper.createObjectNode();
        dataNode1.put("name", "file1.vcf");
        dataNode1.put("size", 12345L);
        ObjectNode dataNode2 = mapper.createObjectNode();
        dataNode2.put("name", "file2.vcf.gz");
        dataNode2.put("size", 67890L);

        dataNodeArray.add(dataNode1);
        dataNodeArray.add(dataNode2);

        globusRootNode.put("DATA", dataNodeArray);
        return metadataRootNode;

    }

    private void assertEmailsSentToUserAndHelpDesk(boolean shouldContainConsentStatement, boolean deprecatedVersion) throws JsonProcessingException {
        String mailhogUrl = "http://" + mailhog.getHost() + ":" + mailhog.getMappedPort(8025) + "/api/v2/messages";
        RestTemplate restTemplate = new RestTemplate();
        JsonNode mailHogResponse = new ObjectMapper().readTree(restTemplate.getForObject(mailhogUrl, String.class));
        JsonNode emailItems = mailHogResponse.get("items");
        assertThat(emailItems).isNotEmpty();

        List<JsonNode> emailList = StreamSupport.stream(emailItems.spliterator(), false)
                .collect(Collectors.toList());

        // Assert email sent to EVA Helpdesk
        List<JsonNode> helpdeskEmails = emailList.stream()
                .filter(email -> {
                    JsonNode headers = email.path("Content").path("Headers");
                    List<String> toList = getStringList(headers.path("To"));
                    List<String> fromList = getStringList(headers.path("From"));
                    List<String> subjectList = getStringList(headers.path("Subject"));

                    return toList.contains(evaHelpdeskEmail)
                            && fromList.contains("eva-noreply@ebi.ac.uk")
                            && subjectList.stream().anyMatch(subject ->
                            subject.contains("New Submission Uploaded. Submission Id - (" + submissionId + ")"));
                })
                .collect(Collectors.toList());

        assertEquals(1, helpdeskEmails.size());
        String helpDeskEmailContent = helpdeskEmails.get(0).path("Content").path("Body").asText();
        assertTrue(helpDeskEmailContent.replaceAll("=\\r?\\n", "").contains("User Primary Email: " + webinUserPrimaryEmail));
        assertTrue(helpDeskEmailContent.replaceAll("=\\r?\\n", "").contains("User Secondary Emails: " +
                String.join(", ", webinUserSecondaryEmails)));

        // Assert email sent to Webin User
        List<JsonNode> userEmails = emailList.stream()
                .filter(email -> {
                    JsonNode headers = email.path("Content").path("Headers");
                    String body = email.path("Content").path("Body").asText();

                    List<String> toList = getStringList(headers.path("To"));
                    List<String> fromList = getStringList(headers.path("From"));
                    List<String> ccList = getStringList(headers.path("Cc")).stream()
                            .flatMap(s -> Arrays.stream(s.split(",")).map(String::trim))
                            .collect(Collectors.toList());
                    List<String> subjectList = getStringList(headers.path("Subject"));

                    return toList.contains(webinUserPrimaryEmail)
                            && ccList.contains(webinUserSecondaryEmails.get(0))
                            && ccList.contains(webinUserSecondaryEmails.get(1))
                            && fromList.contains(evaHelpdeskEmail)
                            && subjectList.stream().anyMatch(subject ->
                            subject.contains("EVA Submission Update: UPLOADED SUCCESS"))
                            && body.replaceAll("=\\r?\\n", "").contains("Here is the update for your submission: <br /><br />Submission ID: " + submissionId + "<br />");
                })
                .collect(Collectors.toList());

        assertEquals(1, userEmails.size());


        String body = userEmails.get(0).path("Content").path("Body").asText();
        if (shouldContainConsentStatement) {
            assertTrue(body.replaceAll("=\\r?\\n", "").contains("Your submission contains human genotypes for which we require a Consent Statement"));
        } else {
            assertFalse(body.replaceAll("=\\r?\\n", "").contains("Your submission contains human genotypes for which we require a Consent Statement"));
        }

        if (deprecatedVersion) {
            assertTrue(body.replaceAll("=\\r?\\n", "").contains("You are using a deprecated version of eva-sub-cli. Please upgrade to the latest version to avoid future submission failures."));
        } else {
            assertFalse(body.replaceAll("=\\r?\\n", "").contains("You are using a deprecated version of eva-sub-cli. Please upgrade to the latest version to avoid future submission failures."));
        }
    }


    private static List<String> getStringList(JsonNode node) {
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }
}
