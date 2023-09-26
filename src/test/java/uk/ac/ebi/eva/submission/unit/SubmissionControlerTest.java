package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import uk.ac.ebi.eva.submission.controller.SubmissionController;
import uk.ac.ebi.eva.submission.service.*;


import static org.mockito.Mockito.when;

public class SubmissionControlerTest {


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
        String token = "lsriUserToken";
        when(webinTokenService.getWebinUserIdFromToken(token)).thenReturn(null);
        when(lsriTokenService.getLsriUserIdFromToken(token)).thenReturn(userId);
        userId = submissionController.getUserId(token);
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(userId);
    }

    @Test
    public void testGetUserIdWebin() throws Exception {
        String userId = "lsriuser@lsri.com";
        String token = "lsriUserToken";
        when(webinTokenService.getWebinUserIdFromToken(token)).thenReturn(userId);
        userId = submissionController.getUserId(token);
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(userId);
    }
}