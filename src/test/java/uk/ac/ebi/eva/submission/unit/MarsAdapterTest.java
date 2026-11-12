package uk.ac.ebi.eva.submission.unit;

import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.receipt.MarsAccession;
import com.elixir.mars.repository.models.receipt.MarsPath;
import com.elixir.mars.repository.models.receipt.MarsReceipt;
import com.elixir.mars.repository.models.receipt.MarsSubmissionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.eva.submission.entity.SubmissionTrackingDetails;
import uk.ac.ebi.eva.submission.entity.SubmittedAnalysis;
import uk.ac.ebi.eva.submission.service.MarsAdapter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarsAdapterTest {

    private final MarsAdapter service = new MarsAdapter();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void successfulReceiptMatchesAliasesRatherThanSubmissionOrder() throws Exception {
        IsaJson isa = fixture();
        MarsReceipt receipt = service.buildMarsSuccessfulReceipt(tracking(), isa);

        assertEquals("eva", receipt.getTargetRepository());
        assertEquals(MarsSubmissionStatus.SUCCESS, receipt.getSubmissionStatus());
        assertEquals(4, receipt.getAccessions().size());
        MarsAccession project = receipt.getAccessions().getFirst();
        assertEquals("PRJEB1", project.getValue());
        assertEquals(1, project.getPath().size());
        assertPath(project.getPath().getFirst(), "studies", "identifier", "study-1");
        assertFileAccession(receipt.getAccessions().get(1), "ERZ1", "assay-1", "file-1");
        assertFileAccession(receipt.getAccessions().get(2), "ERZ1", "assay-1", "file-2");
        assertFileAccession(receipt.getAccessions().get(3), "ERZ2", "assay-2", "file-3");
    }

    @Test
    void conversionUsesTheSameAliasesAndKeepsFilesFromAllAssays() throws Exception {
        JsonNode result = service.convertMarsToEvaJson(fixture());
        assertEquals(3, result.get("files").size());
        assertEquals("alias-a", result.at("/analysis/0/analysisAlias").asText());
        assertEquals("alias-b", result.at("/analysis/2/analysisAlias").asText());
        assertEquals("First analysis", result.at("/analysis/0/analysisTitle").asText());
    }

    @Test
    void conversionMapsProjectAnalysisAndFileMetadata() throws Exception {
        ObjectNode isa = mapper.valueToTree(fixture());
        ObjectNode study = (ObjectNode) isa.at("/studies/0");
        study.set("comments", mapper.readTree("""
                [{"name": "TaxonomyId", "value": "9606"},
                 {"name": "CENTRE", "value": "EBI"}]
                """));
        ((ArrayNode) study.at("/protocols/0/parameters")).addAll(
                (ArrayNode) mapper.readTree("""
                [{"@id": "description", "parameterName": {"annotationValue": "description"}},
                 {"@id": "type", "parameterName": {"annotationValue": "experimentType"}},
                 {"@id": "reference", "parameterName": {"annotationValue": "reference"}}]
                """));
        ((ArrayNode) isa.at("/studies/0/assays/0/processSequence/0/parameterValues")).addAll(
                (ArrayNode) mapper.readTree("""
                [{"category": {"@id": "reference"}, "value": {"annotationValue": "GCA_000001405.29"}},
                 {"category": {"@id": "description"}, "value": {"annotationValue": "Variant calls"}},
                 {"category": {"@id": "type"}, "value": {"annotationValue": "whole_genome_sequencing"}}]
                """));
        ((ObjectNode) isa.at("/studies/0/assays/0/dataFiles/0")).set("comments", mapper.readTree("""
                [{"name": "md5", "value": "0123456789abcdef0123456789abcdef"},
                 {"name": "FILESIZE", "value": "4294967296"}]
                """));

        JsonNode result = service.convertMarsToEvaJson(mapper.treeToValue(isa, IsaJson.class));

        assertEquals(mapper.readTree("""
                {"title": "Project", "description": "Description", "taxId": 9606, "centre": "EBI"}
                """), result.get("project"));
        assertEquals(mapper.readTree("""
                {"analysisTitle": "First analysis", "analysisAlias": "alias-a",
                 "description": "Variant calls", "experimentType": "WHOLE_GENOME_SEQUENCING",
                 "referenceGenome": "GCA_000001405.29"}
                """), result.at("/analysis/0"));
        assertEquals(mapper.readTree("""
                {"analysisAlias": "alias-a", "fileName": "first.vcf",
                 "md5": "0123456789abcdef0123456789abcdef", "fileSize": 4294967296}
                """), result.at("/files/0"));
    }

    @Test
    void conversionFindsSamplesThroughInputsAndPreviousProcessesWithoutRevisitingCycles() throws Exception {
        ObjectNode isa = mapper.valueToTree(fixture());
        ObjectNode study = (ObjectNode) isa.at("/studies/0");
        study.set("characteristicCategories", mapper.readTree("""
                [{"@id": "vcf-name", "characteristicType": {"annotationValue": "sampleInVcf"}},
                 {"@id": "accession", "characteristicType": {"annotationValue": "accession"}}]
                """));
        ((ObjectNode) study.get("materials")).set("samples", mapper.readTree("""
                [{"@id": "sample-1", "characteristics": [
                    {"category": {"@id": "vcf-name"}, "value": {"annotationValue": "donor-1"}},
                    {"category": {"@id": "accession"}, "value": {"annotationValue": "SAMEA1"}}]},
                 {"@id": "sample-2", "characteristics": [
                    {"category": {"@id": "vcf-name"}, "value": {"annotationValue": "donor-2"}},
                    {"category": {"@id": "accession"}, "value": {"annotationValue": "SAMEA2"}}]}]
                """));
        ObjectNode assay = (ObjectNode) study.at("/assays/0");
        assay.withArray("dataFiles").remove(1);
        ObjectNode process = (ObjectNode) assay.at("/processSequence/0");
        process.set("inputs", mapper.readTree("""
                [{"@id": "intermediate"}]
                """));
        process.set("previousProcess", mapper.readTree("""
                {"@id": "previous"}
                """));
        assay.withArray("processSequence").addAll(
                (ArrayNode) mapper.readTree("""
                [{"@id": "upstream", "inputs": [{"@id": "sample-1"}],
                  "outputs": [{"@id": "intermediate"}], "previousProcess": {"@id": "process-1"}},
                 {"@id": "previous", "inputs": [{"@id": "sample-2"}], "outputs": []}]
                """));

        JsonNode result = service.convertMarsToEvaJson(mapper.treeToValue(isa, IsaJson.class));

        assertEquals(mapper.readTree("""
                [{"analysisAlias": "alias-a", "sampleInVcf": "donor-1", "bioSampleAccession": "SAMEA1"},
                 {"analysisAlias": "alias-a", "sampleInVcf": "donor-2", "bioSampleAccession": "SAMEA2"}]
                """), result.get("sample"));
    }

    @Test
    void missingAliasMatchFailsRatherThanReturningIncompleteSuccess() throws Exception {
        SubmissionTrackingDetails tracking = tracking();
        tracking.setAnalyses(List.of(tracking.getAnalyses().getFirst()));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.buildMarsSuccessfulReceipt(tracking, fixture()));
        assertTrue(error.getMessage().contains("alias-a"));
    }

    @Test
    void missingAccessionFailsRatherThanReturningIncompleteSuccess() throws Exception {
        SubmissionTrackingDetails tracking = tracking();
        tracking.getAnalyses().getFirst().setAnalysisAccession(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.buildMarsSuccessfulReceipt(tracking, fixture()));
    }

    @Test
    void missingProjectAccessionFails() throws Exception {
        SubmissionTrackingDetails tracking = tracking();
        tracking.setProjectAccession(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.buildMarsSuccessfulReceipt(tracking, fixture()));
    }

    @Test
    void duplicateAnalysisAliasesFail() throws Exception {
        SubmissionTrackingDetails tracking = tracking();
        SubmittedAnalysis analysis = tracking.getAnalyses().getFirst();
        tracking.setAnalyses(List.of(analysis, analysis));
        assertThrows(IllegalArgumentException.class,
                () -> service.buildMarsSuccessfulReceipt(tracking, fixture()));
    }

    @Test
    void conflictingAccessionsForTheSameFileIdFail() throws Exception {
        IsaJson isa = fixture();
        var assay = isa.getStudies().getFirst().getAssays().get(1);
        assay.getDataFiles().getFirst().setId("file-1");
        assay.getProcessSequence().getFirst().getOutputs().getFirst().setId("file-1");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.buildMarsSuccessfulReceipt(tracking(), isa));
        assertTrue(error.getMessage().contains("Conflicting accessions for data file ID"));
    }

    @Test
    void repeatedInvocationsDoNotRetainAccessions() throws Exception {
        service.buildMarsSuccessfulReceipt(tracking(), fixture());
        SubmissionTrackingDetails tracking = tracking();
        tracking.setProjectAccession("PRJEB2");
        tracking.getAnalyses().get(1).setAnalysisAccession("ERZ3");
        MarsReceipt receipt = service.buildMarsSuccessfulReceipt(tracking, fixture());
        assertEquals(4, receipt.getAccessions().size());
        assertEquals("PRJEB2", receipt.getAccessions().getFirst().getValue());
        assertEquals("ERZ3", receipt.getAccessions().get(1).getValue());
    }

    private void assertFileAccession(MarsAccession accession, String value, String assayId, String fileId) {
        assertEquals(value, accession.getValue());
        assertEquals(3, accession.getPath().size());
        assertPath(accession.getPath().get(0), "studies", "title", "Project");
        assertPath(accession.getPath().get(1), "assays", "@id", assayId);
        assertPath(accession.getPath().get(2), "dataFiles", "@id", fileId);
    }

    private void assertPath(MarsPath path, String key, String whereKey, String whereValue) {
        assertEquals(key, path.getKey());
        assertEquals(whereKey, path.getWhere().getKey());
        assertEquals(whereValue, path.getWhere().getValue());
    }

    private SubmissionTrackingDetails tracking() {
        SubmissionTrackingDetails tracking = new SubmissionTrackingDetails("submission-1");
        tracking.setProjectAccession("PRJEB1");
        SubmittedAnalysis first = new SubmittedAnalysis("submission-1", "alias-a");
        first.setAnalysisAccession("ERZ1");
        SubmittedAnalysis second = new SubmittedAnalysis("submission-1", "alias-b");
        second.setAnalysisAccession("ERZ2");
        tracking.setAnalyses(List.of(second, first));
        return tracking;
    }

    private IsaJson fixture() throws Exception {
        return mapper.readValue("""
                {"studies": [{
                  "identifier": "study-1", "title": "Project", "description": "Description",
                  "comments": [], "people": [], "characteristicCategories": [],
                  "materials": {"samples": []},
                  "protocols": [{"@id": "protocol-1", "parameters": [
                    {"@id": "alias", "parameterName": {"annotationValue": "AnalysisAlias"}},
                    {"@id": "title", "parameterName": {"annotationValue": "analysisTitle"}}
                  ]}],
                  "assays": [{
                    "@id": "assay-1",
                    "dataFiles": [
                      {"@id": "file-1", "name": "first.vcf", "comments": []},
                      {"@id": "file-2", "name": "second.vcf", "comments": []}
                    ],
                    "processSequence": [{
                      "@id": "process-1", "executesProtocol": {"@id": "protocol-1"},
                      "inputs": [], "outputs": [{"@id": "file-1"}, {"@id": "file-2"}],
                      "parameterValues": [
                        {"category": {"@id": "alias"}, "value": {"annotationValue": "alias-a"}},
                        {"category": {"@id": "title"}, "value": {"annotationValue": "First analysis"}}
                      ]
                    }]
                  }, {
                    "@id": "assay-2",
                    "dataFiles": [{"@id": "file-3", "name": "third.vcf", "comments": []}],
                    "processSequence": [{
                      "@id": "process-2", "executesProtocol": {"@id": "protocol-1"},
                      "inputs": [], "outputs": [{"@id": "file-3"}],
                      "parameterValues": [
                        {"category": {"@id": "alias"}, "value": {"annotationValue": "alias-b"}}
                      ]
                    }]
                  }]
                }]}
                """, IsaJson.class);
    }
}
