package uk.ac.ebi.eva.submission.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SchemaDownloader {
    public static String TAG_URL = "https://api.github.com/repos/EBIvariation/eva-sub-cli/tags";
    public static String SCHEMA_URL = "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/{tag}/eva_sub_cli/etc/call_home_payload_schema.json";

    private final RestTemplate restTemplate;

    public SchemaDownloader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String getLatestTag(String tagURL) {
        JsonNode tagJson = restTemplate.getForObject(tagURL, JsonNode.class);
        return tagJson.get(0).get("name").asText();
    }

    @Cacheable(value = "schemaCache", key = "#schemaUrl")
    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String loadSchemaFromGitHub(String schemaUrl) {
        return restTemplate.getForObject(schemaUrl, String.class);
    }

    @CacheEvict(value = "schemaCache", allEntries = true)
    @Scheduled(fixedRate = 48 * 60 * 60 * 1000)
    public void evictSchemaCache() {
    }
}
