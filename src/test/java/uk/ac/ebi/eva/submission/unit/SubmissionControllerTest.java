package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import uk.ac.ebi.eva.submission.controller.SubmissionController;
import uk.ac.ebi.eva.submission.model.SubmissionUser;
import uk.ac.ebi.eva.submission.service.*;


import static org.mockito.Mockito.when;

public class SubmissionControllerTest {


    @Mock
    private WebinTokenService webinTokenService;

    @Mock
    private LsriTokenService lsriTokenService;

    @InjectMocks
    private SubmissionController submissionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetUserIdLSRI() throws Exception {
        String userId = "lsriuser@lsri.com";
        String loginType = LoginMethod.LSRI.getLoginType();
        String token = "lsriUserToken";
        SubmissionUser user = new SubmissionUser(userId, loginType);
        when(webinTokenService.getWebinUserFromToken(token)).thenReturn(null);
        when(lsriTokenService.getLsriUserFromToken(token)).thenReturn(user);

        SubmissionUser submissionUser = submissionController.getUser(token);
        assertThat(submissionUser.getUserId()).isNotNull();
        assertThat(submissionUser.getUserId()).isEqualTo(userId);
    }

    @Test
    public void testGetUserIdWebin() throws Exception {
        String userId = "lsriuser@lsri.com";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String token = "lsriUserToken";
        SubmissionUser user = new SubmissionUser(userId, loginType);
        when(webinTokenService.getWebinUserFromToken(token)).thenReturn(user);
        SubmissionUser submissionUser = submissionController.getUser(token);
        assertThat(submissionUser.getUserId()).isNotNull();
        assertThat(submissionUser.getUserId()).isEqualTo(userId);
    }
}