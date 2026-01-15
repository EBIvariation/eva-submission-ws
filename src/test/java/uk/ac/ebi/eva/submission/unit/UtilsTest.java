package uk.ac.ebi.eva.submission.unit;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.eva.submission.util.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void testExtractVersionFromSchemaURL_shouldExtractVersion() {
        // should extract the RELEASE version
        String url = "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/refs/tags/v0.4.14/eva_sub_cli/etc/eva_schema.json";
        String version = Utils.extractVersionFromSchemaUrl(url);
        assertEquals("v0.4.14", version);

        // should extract the SNAPSHOT version
        url = "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/refs/tags/v1.2.3-SNAPSHOT/schema.json";
        version = Utils.extractVersionFromSchemaUrl(url);
        assertEquals("v1.2.3-SNAPSHOT", version);

        // should extract the DEV version
        url = "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/refs/tags/v2.10.7.dev1/schema.json";
        version = Utils.extractVersionFromSchemaUrl(url);
        assertEquals("v2.10.7.dev1", version);
    }

    @Test
    void testExtractVersionFromSchemaURL_shouldThrowException() {
        String url = "https://raw.githubusercontent.com/EBIvariation/eva-sub-cli/main/schema.json";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Utils.extractVersionFromSchemaUrl(url)
        );

        assertTrue(ex.getMessage().contains("Version not found"));
    }

    @Test
    void testCompareVersions() {
        assertEquals(0, Utils.compareVersions("v1.2.3", "v1.2.3"));
        assertTrue(Utils.compareVersions("v2.0.0", "v1.9.9") > 0);
        assertTrue(Utils.compareVersions("v1.10.0", "v1.2.9") > 0);
        assertTrue(Utils.compareVersions("v1.2.4", "v1.2.3") > 0);
        assertTrue(Utils.compareVersions("v1.2.3-SNAPSHOT", "v1.2.3") < 0);

        assertEquals(0, Utils.compareVersions("v1.2.3", "1.2.3"));
        assertTrue(Utils.compareVersions("2.0.0", "v1.9.9") > 0);
        assertTrue(Utils.compareVersions("v1.10.0", "1.2.9") > 0);
    }
}
