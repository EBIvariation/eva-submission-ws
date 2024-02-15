package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.service.LoginMethod;
import uk.ac.ebi.eva.submission.service.LsriTokenService;
import uk.ac.ebi.eva.submission.service.WebinTokenService;

import static org.assertj.core.api.Assertions.assertThat;
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
    public void testGetUserIdLSRI() {
        String userId = "lsriuser@lsri.com";
        String loginType = LoginMethod.LSRI.getLoginType();
        String token = "lsriUserToken";
        SubmissionAccount account = new SubmissionAccount(userId, loginType);
        when(webinTokenService.getWebinUserAccountFromToken(token)).thenReturn(null);
        when(lsriTokenService.getLsriUserAccountFromToken(token)).thenReturn(account);

        SubmissionAccount submissionAccount = submissionController.getSubmissionAccount(token);
        assertThat(submissionAccount.getUserId()).isNotNull();
        assertThat(submissionAccount.getUserId()).isEqualTo(userId);
    }

    @Test
    public void testGetUserIdWebin() {
        String userId = "lsriuser@lsri.com";
        String loginType = LoginMethod.WEBIN.getLoginType();
        String token = "lsriUserToken";
        SubmissionAccount account = new SubmissionAccount(userId, loginType);
        when(webinTokenService.getWebinUserAccountFromToken(token)).thenReturn(account);

        SubmissionAccount submissionAccount = submissionController.getSubmissionAccount(token);
        assertThat(submissionAccount.getUserId()).isNotNull();
        assertThat(submissionAccount.getUserId()).isEqualTo(userId);
    }
}