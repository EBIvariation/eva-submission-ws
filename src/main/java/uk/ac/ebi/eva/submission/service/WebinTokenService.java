package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.model.SubmissionUser;

@Service
public class WebinTokenService {

    @Value("${webin.userinfo.url}")
    private String userInfoUrl;

    public SubmissionUser getWebinUserFromToken(String userToken) {
        return TokenServiceUtil.getUser(userToken, this.userInfoUrl, LoginMethod.WEBIN);
    }
}
