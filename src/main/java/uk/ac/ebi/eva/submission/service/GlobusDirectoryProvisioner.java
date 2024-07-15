package uk.ac.ebi.eva.submission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Service
public class GlobusDirectoryProvisioner {

    private final GlobusTokenRefreshService globusTokenRefreshService;

    private final RestTemplate restTemplate;

    @Value("${globus.submission.endpointId}")
    private String endpointId;

    private static final String GLOBUS_TRANSFER_API_BASE_URL =
            "https://transfer.api.globusonline.org/v0.10/operation/endpoint";


    public GlobusDirectoryProvisioner(GlobusTokenRefreshService globusTokenRefreshService, RestTemplate restTemplate) {
        this.globusTokenRefreshService = globusTokenRefreshService;
        this.restTemplate = restTemplate;
    }

    public void createSubmissionDirectory(String directoryToCreate) {
        String fileSeparator = System.getProperty("file.separator");
        String[] directoriesToCreate = directoryToCreate.split(Pattern.quote(fileSeparator));
        StringBuilder directoryPathSoFar = new StringBuilder();
        for (String directory : directoriesToCreate) {
            directoryPathSoFar.append(directory);
            createDirectory(directoryPathSoFar.toString());
            directoryPathSoFar.append(fileSeparator);
        }
    }

    private void createDirectory(String directoryToCreate) {
        HttpHeaders headers = getGlobusAccessHeaders();

        if (this.alreadyExists(headers, directoryToCreate)) {
            return;
        }

        String transferApiUrl = String.format("%s/%s/mkdir", GLOBUS_TRANSFER_API_BASE_URL, endpointId);
        // Create the request body with the endpoint ID and the path for the new directory
        String requestBody = String.format("{\"DATA_TYPE\": \"mkdir\", \"path\": \"/%s\"}", directoryToCreate);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(transferApiUrl, HttpMethod.POST, requestEntity,
                                                                String.class);

        // Check the response status and handle errors if necessary
        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.printf("Directory '%s' created successfully%n", directoryToCreate);
        } else {
            System.out.printf("Failed to create directory '%s': %s", directoryToCreate, response.getStatusCode());
        }
    }

    private boolean alreadyExists(HttpHeaders headers, String directoryName) {
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        try {
            restTemplate.exchange(
                    GLOBUS_TRANSFER_API_BASE_URL + "/" + endpointId + "/ls?path=" + directoryName,
                    HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException ex) {
            return false;
        }
    }

    public String listSubmittedFiles(String submissionDirPath) {
        HttpEntity<String> requestEntity = new HttpEntity<>(getGlobusAccessHeaders());
        String transferApiUrl = String.format("%s/%s/ls?path=%s", GLOBUS_TRANSFER_API_BASE_URL, endpointId, submissionDirPath);
        ResponseEntity<String> response = restTemplate.exchange(transferApiUrl, HttpMethod.GET, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.printf("Directory %s listed successfully%n", submissionDirPath);
            return response.getBody();
        } else {
            System.out.printf("Failed to retrieve directory '%s': %s", submissionDirPath, response.getStatusCode());
            return "";
        }
    }

    private HttpHeaders getGlobusAccessHeaders() {
        String accessToken = globusTokenRefreshService.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");

        return headers;
    }


}
