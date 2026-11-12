package uk.ac.ebi.eva.submission.service;

import com.elixir.mars.repository.MarsReceiptException;
import com.elixir.mars.repository.MarsReceiptProvider;
import com.elixir.mars.repository.ReceiptAccessionsMap;
import com.elixir.mars.repository.models.isa.Assay;
import com.elixir.mars.repository.models.isa.Characteristic;
import com.elixir.mars.repository.models.isa.CharacteristicCategory;
import com.elixir.mars.repository.models.isa.CharacteristicType;
import com.elixir.mars.repository.models.isa.Comment;
import com.elixir.mars.repository.models.isa.DataFile;
import com.elixir.mars.repository.models.isa.Input;
import com.elixir.mars.repository.models.isa.IsaJson;
import com.elixir.mars.repository.models.isa.Parameter;
import com.elixir.mars.repository.models.isa.ParameterValue;
import com.elixir.mars.repository.models.isa.ProcessSequence;
import com.elixir.mars.repository.models.isa.Protocol;
import com.elixir.mars.repository.models.isa.Sample;
import com.elixir.mars.repository.models.isa.Study;
import com.elixir.mars.repository.models.receipt.MarsReceipt;
import com.elixir.mars.repository.models.receipt.MarsSubmissionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import uk.ac.ebi.eva.submission.entity.SubmissionTrackingDetails;
import uk.ac.ebi.eva.submission.entity.SubmittedAnalysis;
import uk.ac.ebi.eva.submission.model.metadata.Analysis;
import uk.ac.ebi.eva.submission.model.metadata.EvaMetadata;
import uk.ac.ebi.eva.submission.model.metadata.ExperimentType;
import uk.ac.ebi.eva.submission.model.metadata.File;
import uk.ac.ebi.eva.submission.model.metadata.Project;
import uk.ac.ebi.eva.submission.model.metadata.EvaSample;
import uk.ac.ebi.eva.submission.model.metadata.SubmitterDetails;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.elixir.mars.repository.IsaJsonGraphLookup.findProcessByOutputId;
import static com.elixir.mars.repository.IsaJsonGraphLookup.findSubmissionId;

@Service
public class MarsAdapter extends MarsReceiptProvider {

    private static final String REPOSITORY_ID = "eva";

    // Names of Comments and Parameters - must be kept in sync with ISA-JSON producer
    private static final String TAXONOMY_ID_KEY = "taxonomyId";
    private static final String CENTRE_KEY = "centre";
    private static final String MD5_CHECKSUM_KEY = "MD5";
    private static final String FILE_SIZE_KEY = "fileSize";
    private static final String ANALYSIS_TITLE_KEY = "analysisTitle";
    private static final String ANALYSIS_ALIAS_KEY = "analysisAlias";
    private static final String ANALYSIS_DESCRIPTION_KEY = "description";
    private static final String EXPERIMENT_TYPE_KEY = "experimentType";
    private static final String REFERENCE_GENOME_KEY = "reference";
    private static final String SAMPLE_IN_VCF_KEY = "sampleInVcf";
    private static final String SAMPLE_ACCESSION_KEY = "accession";

    public MarsAdapter() {
        super(REPOSITORY_ID);
    }

    public JsonNode convertMarsToEvaJson(IsaJson marsIsaJson) {
        EvaMetadata evaMetadata = new EvaMetadata();

        // Assume single study
        MarsMapping mapping = mapMarsEntities(marsIsaJson);
        Study study = mapping.study();

        // Study level maps to help find entities by ID
        Map<String, Sample> sampleMap = new HashMap<>();
        for (Sample sample : study.getMaterials().getSamples()) {
            sampleMap.put(sample.getId(), sample);
        }
        Map<String, CharacteristicType> charCatMap = new HashMap<>();
        for (CharacteristicCategory cc : study.getCharacteristicCategories()) {
            charCatMap.put(cc.getId(), cc.getCharacteristicType());
        }

        // Create Submitter Details
        List<SubmitterDetails> submitterDetails = study.getPeople().stream().map(
                person -> new SubmitterDetails(
                        person.getLastName(), person.getFirstName(), person.getPhone(), person.getEmail(),
                        // TODO laboratory vs. centre?
                        person.getAffiliation(), person.getAffiliation(), person.getAddress())).collect(
                Collectors.toList());
        evaMetadata.setSubmitterDetails(submitterDetails);

        // Create Project
        // Assume taxonomy ID and centre are study comments
        Integer taxonomyId = null;
        String centre = null;
        for (Comment comment : study.getComments()) {
            if (comment.getName().equalsIgnoreCase(TAXONOMY_ID_KEY)) {
                taxonomyId = Integer.valueOf(comment.getValue());
            } else if (comment.getName().equalsIgnoreCase(CENTRE_KEY)) {
                centre = comment.getValue();
            }
        }
        evaMetadata.setProject(new Project(study.getTitle(), study.getDescription(), taxonomyId, centre));

        // Assume ISA is already filtered to only include EVA assays
        List<Analysis> analyses = new ArrayList<>();
        List<File> files = new ArrayList<>();
        List<EvaSample> samples = new ArrayList<>();
        for (Assay assay : study.getAssays()) {

            Map<String, ProcessSequence> processMap = new HashMap<>();
            for (ProcessSequence process : assay.getProcessSequence()) {
                processMap.put(process.getId(), process);
            }

            // Start with data files
            for (AnalysisMapping analysisMapping : mapping.analyses()) {
                if (analysisMapping.assay() != assay) {
                    continue;
                }
                DataFile dataFile = analysisMapping.dataFile();
                String md5checksum = null;
                Long fileSize = null;
                for (Comment comment :  dataFile.getComments()) {
                    if (comment.getName().equalsIgnoreCase(MD5_CHECKSUM_KEY)) {
                        md5checksum = comment.getValue();
                    }
                    if (comment.getName().equalsIgnoreCase(FILE_SIZE_KEY)) {
                        fileSize = Long.valueOf(comment.getValue());
                    }
                }

                // Create Analysis from process sequence
                ProcessSequence process = analysisMapping.process();
                String analysisTitle = analysisMapping.parameter(ANALYSIS_TITLE_KEY);
                String analysisAlias = analysisMapping.parameter(ANALYSIS_ALIAS_KEY);
                String analysisDescription = analysisMapping.parameter(ANALYSIS_DESCRIPTION_KEY);
                String experimentTypeValue = analysisMapping.parameter(EXPERIMENT_TYPE_KEY);
                ExperimentType experimentType = experimentTypeValue == null ? null
                        : ExperimentType.valueOf(experimentTypeValue.toUpperCase(Locale.ROOT));
                String referenceGenome = analysisMapping.parameter(REFERENCE_GENOME_KEY);

                files.add(new File(analysisAlias, dataFile.getName(), md5checksum, fileSize));
                analyses.add(new Analysis(analysisTitle, analysisAlias, analysisDescription, experimentType,
                                          referenceGenome));

                // End with samples
                // Walk back via process input/output or previous process links, to be sure to collect all sample inputs
                ArrayDeque<ProcessSequence> processesToVisit = new ArrayDeque<>();
                Set<String> visitedProcessIds = new HashSet<>();
                processesToVisit.add(process);
                while (!processesToVisit.isEmpty()) {
                    ProcessSequence currentProcess = processesToVisit.removeFirst();
                    if (!visitedProcessIds.add(currentProcess.getId())) {
                        continue;
                    }
                    for (Input input : currentProcess.getInputs()) {
                        if (sampleMap.containsKey(input.getId())) {
                            Sample sampleToAdd = sampleMap.get(input.getId());
                            String sampleInVcf = null;
                            String bioSampleAccession = null;
                            for (Characteristic characteristic : sampleToAdd.getCharacteristics()) {
                                String characteristicName = charCatMap.get(characteristic.getCategory().getId())
                                                                      .getAnnotationValue();
                                if (characteristicName.equalsIgnoreCase(SAMPLE_IN_VCF_KEY)) {
                                    sampleInVcf = characteristic.getValue().getAnnotationValue();
                                }
                                if (characteristicName.equalsIgnoreCase(SAMPLE_ACCESSION_KEY)) {
                                    bioSampleAccession = characteristic.getValue().getAnnotationValue();
                                }
                            }
                            samples.add(new EvaSample(analysisAlias, sampleInVcf, bioSampleAccession));
                        }
                        // Add process that produced this input
                        ProcessSequence producer = findProcessByOutputId(assay.getProcessSequence(), input.getId());
                        if (producer != null) {
                            processesToVisit.add(producer);
                        }
                    }
                    // Add previous process
                    if (currentProcess.getPreviousProcess() != null) {
                        ProcessSequence previous = processMap.get(currentProcess.getPreviousProcess().getId());
                        if (previous != null) {
                            processesToVisit.add(previous);
                        }
                    }
                }
            }
        }
        evaMetadata.setFiles(files);
        evaMetadata.setSamples(samples);
        evaMetadata.setAnalyses(analyses);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(evaMetadata);
    }

    public MarsReceipt buildMarsPendingReceipt() {
        return buildMarsReceipt(null, null, null, null, null, null, null, null, MarsSubmissionStatus.PENDING, null);
    }

    public MarsReceipt buildMarsSuccessfulReceipt(SubmissionTrackingDetails trackingDetails, IsaJson isaJson) {
        MarsMapping mapping = mapMarsEntities(isaJson);
        Map<String, String> accessionsByAlias = new HashMap<>();
        for (SubmittedAnalysis analysis : trackingDetails.getAnalyses()) {
            if (analysis.getAnalysisAccession() == null) {
                throw new IllegalArgumentException("Missing accession for analysis: " + analysis.getAnalysisAlias());
            }
            if (accessionsByAlias.putIfAbsent(analysis.getAnalysisAlias(), analysis.getAnalysisAccession()) != null) {
                throw new IllegalArgumentException("Duplicate submitted analysis alias: " + analysis.getAnalysisAlias());
            }
        }
        if (trackingDetails.getProjectAccession() == null) {
            throw new IllegalArgumentException("Missing project accession");
        }

        ReceiptAccessionsMap studiesAccessionsMap = new ReceiptAccessionsMap(
                Study.Fields.identifier, mapping.study().getIdentifier());
        studiesAccessionsMap.accessionMap.put(mapping.study().getIdentifier(), trackingDetails.getProjectAccession());
        ReceiptAccessionsMap dataFilesAccessionsMap = new ReceiptAccessionsMap();
        dataFilesAccessionsMap.isaItemName = DataFile.Fields.id;
        for (AnalysisMapping analysis : mapping.analyses()) {
            String alias = analysis.parameter(ANALYSIS_ALIAS_KEY);
            String accession = accessionsByAlias.get(alias);
            if (accession == null) {
                throw new IllegalArgumentException("No submitted analysis accession for alias: " + alias);
            }
            String previousAccession = dataFilesAccessionsMap.accessionMap.putIfAbsent(
                    analysis.dataFile().getId(), accession);
            if (previousAccession != null && !previousAccession.equals(accession)) {
                throw new IllegalArgumentException("Conflicting accessions for data file ID: "
                        + analysis.dataFile().getId());
            }
        }
        return buildMarsReceipt(studiesAccessionsMap, null, null, null, dataFilesAccessionsMap,
                null, null, null, MarsSubmissionStatus.SUCCESS, isaJson);
    }

    private record AnalysisMapping(Assay assay, DataFile dataFile, ProcessSequence process,
                                   Map<String, String> parameters) {
        private String parameter(String name) {
            return parameters.get(name.toLowerCase(Locale.ROOT));
        }
    }

    private record MarsMapping(Study study, List<AnalysisMapping> analyses) {}

    // Shared entity mapping method for study and analysis, so can be shared between initial metadata submissions and
    // receipt generation
    private MarsMapping mapMarsEntities(IsaJson isaJson) {
        // Assume single study
        Study study = isaJson.getStudies().getFirst();
        Map<String, List<Parameter>> protocolParamsMap = new HashMap<>();
        for (Protocol protocol : study.getProtocols()) {
            protocolParamsMap.put(protocol.getId(), protocol.getParameters());
        }
        List<AnalysisMapping> analyses = new ArrayList<>();
        for (Assay assay : study.getAssays()) {
            for (DataFile dataFile : assay.getDataFiles()) {
                ProcessSequence process = findProcessByOutputId(assay.getProcessSequence(), dataFile.getId());
                if (process == null) {
                    throw new IllegalArgumentException("No producing process for data file: " + dataFile.getId());
                }
                Map<String, String> valuesById = new HashMap<>();
                for (ParameterValue value : process.getParameterValues()) {
                    valuesById.put(value.getCategory().getId(), value.getValue().getAnnotationValue());
                }
                Map<String, String> parameters = new HashMap<>();
                for (Parameter parameter : protocolParamsMap.get(process.getExecutesProtocol().getId())) {
                    parameters.put(parameter.getParameterName().getAnnotationValue().toLowerCase(Locale.ROOT),
                            valuesById.get(parameter.getId()));
                }
                analyses.add(new AnalysisMapping(assay, dataFile, process, parameters));
            }
        }
        return new MarsMapping(study, analyses);
    }

    public MarsReceipt buildMarsFailedReceipt(String message) {
        return buildMarsReceipt(null, null, null, null, null, null, Collections.singletonList(message),null, MarsSubmissionStatus.FAILURE, null);
    }

    public MarsReceipt buildMarsFailedReceipt(MarsReceiptException exception) {
        // TODO - for more detailed errors, i.e. with paths
        return null;
    }

    public String getSubmissionId(IsaJson marsIsaJson) {
        return findSubmissionId(marsIsaJson, REPOSITORY_ID);
    }
}
