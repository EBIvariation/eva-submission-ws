package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebinTokenService {

    @Value("${webin.userinfo.url}")
    private String userInfoUrl;

    public String getWebinUserIdFromToken(String userToken) {
        String tokenAttribute = "id";
        return TokenServiceUtil.getUserId(userToken, this.userInfoUrl, tokenAttribute);
    }
}
