package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class TokenServiceUtil {
    private static final Logger logger = LoggerFactory.getLogger(TokenServiceUtil.class);

    public static String getUserInfoRestResponse(String userToken, String userInfoUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);
            // Parse the response JSON to extract the new access token
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return responseJson.toString();
        } catch (HttpClientErrorException e) {
            logger.warn("Token validation HTTP error: status={}, message={}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            logger.warn("Token validation failed while calling user info endpoint: {}", e.getMessage());
            return null;
        }
    }
}
