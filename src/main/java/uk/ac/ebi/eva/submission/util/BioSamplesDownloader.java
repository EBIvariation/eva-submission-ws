package uk.ac.ebi.eva.submission.util;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BioSamplesDownloader {
    private final RestTemplate restTemplate;

    private static final String BIO_SAMPLES_BASE_URL = "https://www.ebi.ac.uk/biosamples/samples/";

    public BioSamplesDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String downloadSampleFromBioSamples(String accession) {
        return restTemplate.getForObject(BIO_SAMPLES_BASE_URL + accession + ".json", String.class);
    }
}
