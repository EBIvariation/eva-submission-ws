package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.eva.submission.model.SubmissionUser;

import java.util.Objects;


@Service
public class LsriTokenService {
    @Value("${lsri.clientId}")
    private String lsriClientId;

    @Value("${lsri.clientSecret}")
    private String lsriClientSecret;

    @Value("${lsri.userinfo.url}")
    private String userInfoUrl;

    @Value("${lsri.token.url}")
    private String tokenUrl;


    private final Logger logger = LoggerFactory.getLogger(LsriTokenService.class);

    public SubmissionUser getLsriUserFromToken(String userToken) {
        // The only definitive attribute we can expect from querying userInfo is the "sub" attribute
        // See https://connect2id.com/products/server/docs/api/userinfo#claims
        return TokenServiceUtil.getUser(userToken, this.userInfoUrl, LoginMethod.LSRI);
    }

    public String pollForToken(String deviceCode, int maxPollingTimeInSeconds) {
        int pollingIntervalInSeconds = 5;
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < maxPollingTimeInSeconds * 1000L) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(this.lsriClientId, this.lsriClientSecret);

            MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
            map.add("scope", "openid");
            map.add("device_code", deviceCode);
            map.add("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = null;
            try {
                response = restTemplate.postForEntity(this.tokenUrl, request, String.class);
            }
            catch(HttpClientErrorException ex) {
                int statusCode = ex.getRawStatusCode();
                if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                    // The user has not yet approved the access request
                    logger.info("Waiting for user approval...");
                } else {
                    // An error occurred
                    logger.error("An error occurred: " + ex.getResponseBodyAsString());
                    return null;
                }
            }

            if (Objects.nonNull(response) && response.getStatusCode() == HttpStatus.OK) {
                // The user has approved the access request
                try {
                    return new ObjectMapper().readTree(response.getBody()).get("access_token").asText();
                }
                catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }
            }

            try {
                Thread.sleep(pollingIntervalInSeconds * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Stop polling after the maximum polling time
        logger.error("Polling timed out!");
        return null;
    }
}
