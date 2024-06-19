package uk.ac.ebi.eva.submission.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.controller.BaseController;
import uk.ac.ebi.eva.submission.entity.Submission;
import uk.ac.ebi.eva.submission.exception.SubmissionDoesNotExistException;
import uk.ac.ebi.eva.submission.model.SubmissionStatus;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.SubmissionService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import java.util.ArrayList;

@RestController
@RequestMapping("/v1/admin")
public class AdminController extends BaseController {
    private final SubmissionService submissionService;

    public AdminController(SubmissionService submissionService, WebinTokenService webinTokenService,
                           LsriTokenService lsriTokenService) {
        super(webinTokenService, lsriTokenService);
        this.submissionService = submissionService;
    }

    @Operation(summary = "Given a submission id, this endpoint updates the status of the submission to the one provided",
            security = {@SecurityRequirement(name = "basicAuth")
    })
    @Parameters({
            @Parameter(name="submissionId", description = "Id of the submission whose status needs to be updated",
                    required = true, in= ParameterIn.PATH),
            @Parameter(name="status", description = "Desired status of the submission ",
                    required = true, in= ParameterIn.PATH)
    })
    @PutMapping("submission/{submissionId}/status/{status}")
    public ResponseEntity<?> markSubmissionStatus(@PathVariable("submissionId") String submissionId,
                                                  @PathVariable("status") SubmissionStatus status) {
        Submission submission = this.submissionService.markSubmissionStatus(submissionId, status);
        return new ResponseEntity<>(stripUserDetails(submission), HttpStatus.OK);
    }

    @Operation(summary = "This endpoint retrieves all the submissions of a specific status present in the database")
    @Parameters({
            @Parameter(name="status", description = "Desired status of the submission ",
                    required = true, in= ParameterIn.PATH)
    })
    @GetMapping("submissions/status/{status}")
    public ResponseEntity<?> getSubmissionsbyStatus(@PathVariable("status") SubmissionStatus status) {
        try {
            ArrayList<Submission> submissions = (ArrayList<Submission>) submissionService.getSubmissionsByStatus(status);
            return new ResponseEntity<>(submissions, HttpStatus.OK);
        } catch (SubmissionDoesNotExistException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
