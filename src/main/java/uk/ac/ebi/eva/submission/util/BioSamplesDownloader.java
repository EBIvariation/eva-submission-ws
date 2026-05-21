package uk.ac.ebi.eva.submission.util;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Component
public class BioSamplesDownloader {
    private final RestTemplate restTemplate;

    private static final String BIO_SAMPLES_BASE_URL = "https://www.ebi.ac.uk/biosamples/samples/";
    private static final Pattern BIOSAMPLES_ACCESSION = Pattern.compile("^SAM[END][AG]?[0-9]+$");

    public BioSamplesDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String downloadSampleFromBioSamples(String accession) {
        if (!BIOSAMPLES_ACCESSION.matcher(accession).matches()) {
            throw new IllegalArgumentException("Invalid BioSamples accession format: " + accession);
        }
        return restTemplate.getForObject(BIO_SAMPLES_BASE_URL + accession + ".json", String.class);
    }
}
