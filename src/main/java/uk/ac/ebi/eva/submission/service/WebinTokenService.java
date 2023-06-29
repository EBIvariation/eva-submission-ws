package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebinTokenService {
    public String getWebinUserIdFromToken(String userToken) {
        String url = "https://www.ebi.ac.uk/ena/submit/webin/auth/admin/submission-account";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/hal+json");
        headers.set("Content-Type", "application/hal+json");
        headers.set("Authorization", "Bearer " + userToken);

        HttpEntity<String> entity = new HttpEntity<>("", headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        // Parse the response JSON to extract the new access token
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            return responseJson.get("id").asText();
        } catch (Exception e) {
            // Handle errors while parsing the response JSON
            e.printStackTrace();
        }
        return null;
    }
}
