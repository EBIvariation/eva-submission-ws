package uk.ac.ebi.eva.submission.service;

import org.springframework.stereotype.Service;

@Service
public class WebinTokenService {
    public String getWebinUserIdFromToken(String userToken) {
        String userInfoUrl = "https://www.ebi.ac.uk/ena/submit/webin/auth/admin/submission-account";
        String tokenAttribute = "id";
        return TokenServiceUtil.getUserId(userToken, userInfoUrl, tokenAttribute);
    }
}
