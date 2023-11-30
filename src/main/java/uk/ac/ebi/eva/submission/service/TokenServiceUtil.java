package uk.ac.ebi.eva.submission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.eva.submission.model.SubmissionUser;

public class TokenServiceUtil {
    public static SubmissionUser getUser(String userToken, String userInfoUrl, LoginMethod loginMethod) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);
        // Parse the response JSON to extract the new access token
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.getBody());

            if(loginMethod == LoginMethod.WEBIN){
                responseJson = responseJson.get("submissionContacts").get(0);
            }
            String userId = responseJson.get(loginMethod.getUserIdToken()).asText();
            String firstName = responseJson.get(loginMethod.getFirstNameToken()).asText();
            String lastName = responseJson.get(loginMethod.getLastNameToken()).asText();
            String email = responseJson.get(loginMethod.getEmailIdToken()).asText();

            return new SubmissionUser(userId, loginMethod.getLoginType(), firstName, lastName, email);
        } catch (Exception e) {
            // Handle errors while parsing the response JSON
            e.printStackTrace();
        }
        return null;
    }
}
