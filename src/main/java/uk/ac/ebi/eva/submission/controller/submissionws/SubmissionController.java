package uk.ac.ebi.eva.submission.controller.submissionws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.controller.BaseController;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.exception.MetadataFileInfoMismatchException;
import uk.ac.ebi.eva.submission.exception.RequiredFieldsMissingException;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.util.Objects;

import static uk.ac.ebi.eva.submission.entity.SubmissionDetails.PROJECT_DESCRIPTION_LENGTH;
import static uk.ac.ebi.eva.submission.entity.SubmissionDetails.PROJECT_TITLE_LENGTH;

@RestController
@RequestMapping("/v1")
public class SubmissionController extends BaseController {
    private static final String PROJECT = "project";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private final SubmissionService submissionService;
    private final WebinTokenService webinTokenService;
    private final LsriTokenService lsriTokenService;

    public SubmissionController(SubmissionService submissionService, WebinTokenService webinTokenService,
                                LsriTokenService lsriTokenService) {
        super(webinTokenService, lsriTokenService);
        this.submissionService = submissionService;
        this.webinTokenService = webinTokenService;
        this.lsriTokenService = lsriTokenService;
    }

    @Operation(summary = "This endpoint authenticates a user with LSRI")
    @Parameters({
            @Parameter(name = "deviceCode", description = "device code for authentication",
                    required = true, in = ParameterIn.QUERY),
            @Parameter(name = "expiresIn", description = "device code expiration time in seconds",
                    required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("submission/auth/lsri")
    public ResponseEntity<?> authenticateLSRI(@RequestParam("deviceCode") String deviceCode,
                                              @RequestParam("expiresIn") int codeExpirationTimeInSeconds) {
        String token = lsriTokenService.pollForToken(deviceCode, codeExpirationTimeInSeconds);
        if (Objects.nonNull(token)) {
            return new ResponseEntity<>(token, HttpStatus.OK);
        }
        return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    @Operation(summary = "This endpoint marks the initiation of a submission. It will do the necessary prep work " +
            "for receiving the submission files")
    @Parameters({
            @Parameter(name = "Authorization", description = "Token (bearerToken) for authenticating the user",
                    required = true, in = ParameterIn.HEADER)
    })
    @PostMapping("submission/initiate")
    public ResponseEntity<?> initiateSubmission(@RequestHeader("Authorization") String bearerToken) {
        SubmissionAccount submissionAccount = this.getSubmissionAccount(bearerToken);
        if (Objects.isNull(submissionAccount)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        Submission submission = this.submissionService.initiateSubmission(submissionAccount);
        return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
    }

    @Operation(summary = "Given a submission id, this endpoint will mark the submission as uploaded")
    @Parameters({
            @Parameter(name = "Authorization", description = "Token (bearerToken) for authenticating the user",
                    required = true, in = ParameterIn.HEADER),
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be retrieved",
                    required = true, in = ParameterIn.PATH)
    })
    @PutMapping("submission/{submissionId}/uploaded")
    public ResponseEntity<?> markSubmissionUploaded(@RequestHeader("Authorization") String bearerToken,
                                                    @PathVariable("submissionId") String submissionId,
                                                    @RequestBody JsonNode metadataJson) {
        SubmissionAccount submissionAccount = this.getSubmissionAccount(bearerToken);
        if (Objects.isNull(submissionAccount) || !submissionService.checkUserHasAccessToSubmission(submissionAccount, submissionId)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        String submissionStatus = submissionService.getSubmissionStatus(submissionId);
        if (!Objects.equals(submissionStatus, SubmissionStatus.OPEN.toString())) {
            return new ResponseEntity<>(
                    "Submission " + submissionId + " is not in status " + SubmissionStatus.OPEN +
                            ". It cannot be marked as " + SubmissionStatus.UPLOADED +
                            ". Current Status: " + submissionStatus,
                    HttpStatus.BAD_REQUEST);
        }
        try {
            submissionService.checkMetadataFileInfoMatchesWithUploadedFiles(submissionAccount, submissionId, metadataJson);
            try {
                ObjectNode projectNode = (ObjectNode) metadataJson.get(PROJECT);
                String projectTitleOrg = projectNode.get(TITLE).asText();
                String projectDescriptionOrg = projectNode.get(DESCRIPTION).asText();
                projectNode.put(TITLE, projectTitleOrg.substring(0, Math.min(projectTitleOrg.length(), PROJECT_TITLE_LENGTH)));
                projectNode.put(DESCRIPTION, projectDescriptionOrg.substring(0, Math.min(projectDescriptionOrg.length(),
                        PROJECT_DESCRIPTION_LENGTH)));
            } catch (Exception e) {
                throw new RequiredFieldsMissingException("Required fields project title and project description " +
                        "could not be found in metadata json");
            }

            Submission submission = this.submissionService.uploadMetadataJsonAndMarkUploaded(submissionId, metadataJson);

            String projectTitle = metadataJson.get(PROJECT).get(TITLE).asText();
            submissionService.sendMailNotificationToUserForStatusUpdate(submissionAccount, submissionId, projectTitle,
                    SubmissionStatus.UPLOADED, true);
            submissionService.sendMailNotificationToEVAHelpdeskForSubmissionUploaded(submissionAccount, submissionId,
                    projectTitle);
            return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
        } catch (RequiredFieldsMissingException | MetadataFileInfoMismatchException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Given a submission id, this endpoint retrieves the current status of the submission")
    @Parameters({
            @Parameter(name = "submissionId", description = "Id of the submission whose status needs to be retrieved",
                    required = true, in = ParameterIn.PATH)
    })
    @GetMapping("submission/{submissionId}/status")
    public ResponseEntity<?> getSubmissionStatus(@PathVariable("submissionId") String submissionId) {
        try {
            String submissionStatus = submissionService.getSubmissionStatus(submissionId);
            return new ResponseEntity<>(submissionStatus, HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
