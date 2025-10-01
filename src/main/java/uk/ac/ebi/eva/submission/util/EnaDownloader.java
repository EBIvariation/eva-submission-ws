package uk.ac.ebi.eva.submission.util;


import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
public class EnaDownloader {
    private final RestTemplate restTemplate;

    private static final String ENA_BASE_URL = "https://www.ebi.ac.uk/ena/browser/api/xml/";

    public EnaDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Document downloadXmlFromEna(String projectAccession) throws Exception {
        String responseBody = restTemplate.getForObject(ENA_BASE_URL + projectAccession, String.class);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(responseBody)));
    }
}