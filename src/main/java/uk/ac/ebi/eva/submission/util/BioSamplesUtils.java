package uk.ac.ebi.eva.submission.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BioSamplesUtils {
    private final Logger logger = LoggerFactory.getLogger(BioSamplesUtils.class);

    private final BioSamplesDownloader bioSamplesDownloader;

    public BioSamplesUtils(BioSamplesDownloader bioSamplesDownloader) {
        this.bioSamplesDownloader = bioSamplesDownloader;
    }

    public String getTaxIdFromBioSamples(String accession) {
        try {
            String responseBody = bioSamplesDownloader.downloadSampleFromBioSamples(accession);
            JsonNode json = new ObjectMapper().readTree(responseBody);
            return json.path("taxId").asText("");
        } catch (Exception e) {
            logger.error("Error retrieving taxId from BioSamples for accession {}", accession, e);
            return "";
        }
    }
}
