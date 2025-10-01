package uk.ac.ebi.eva.submission.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.Map;

import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.DESCRIPTION;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TAXONOMY_ID;
import static uk.ac.ebi.eva.submission.controller.submissionws.SubmissionController.TITLE;

@Component
public class EnaUtils {
    private final Logger logger = LoggerFactory.getLogger(EnaUtils.class);

    private final EnaDownloader enaDownloader;

    public EnaUtils(EnaDownloader enaDownloader) {
        this.enaDownloader = enaDownloader;
    }

    public Map<String, String> getProjectDetailsFromEna(String projectAccession) {
        Map<String, String> projectDetails = new HashMap<>();
        projectDetails.put(TITLE, "");
        projectDetails.put(DESCRIPTION, "");
        projectDetails.put(TAXONOMY_ID, "");
        try {
            Document xmlDoc = enaDownloader.downloadXmlFromEna(projectAccession);
            projectDetails.put(TITLE, getProjectTitle(xmlDoc));
            projectDetails.put(DESCRIPTION, getProjectDescription(xmlDoc));
            projectDetails.put(TAXONOMY_ID, getProjectTaxonomy(xmlDoc));
        } catch (Exception e) {
            logger.error("Error while getting project details from ENA for project {}", projectAccession, e);
        }
        return projectDetails;
    }

    private String getProjectTitle(Document xmlDoc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String projectTitlePathInXML = "/PROJECT_SET/PROJECT/TITLE";
        NodeList projectTitleNode = (NodeList) xpath.evaluate(projectTitlePathInXML, xmlDoc, XPathConstants.NODESET);
        if (projectTitleNode.getLength() > 0) {
            return projectTitleNode.item(0).getTextContent();
        }

        return "";
    }

    private String getProjectDescription(Document xmlDoc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String projectDescriptionPathInXML = "/PROJECT_SET/PROJECT/DESCRIPTION";
        NodeList projectDescriptionNode = (NodeList) xpath.evaluate(projectDescriptionPathInXML, xmlDoc, XPathConstants.NODESET);
        if (projectDescriptionNode.getLength() > 0) {
            return projectDescriptionNode.item(0).getTextContent();
        }

        return "";
    }

    private String getProjectTaxonomy(Document xmlDoc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        String projectTaxonomyPathInXML = "/PROJECT_SET/PROJECT/SUBMISSION_PROJECT/ORGANISM/TAXON_ID";
        NodeList projectTaxonomyNode = (NodeList) xpath.evaluate(projectTaxonomyPathInXML, xmlDoc, XPathConstants.NODESET);
        if (projectTaxonomyNode.getLength() > 0) {
            return projectTaxonomyNode.item(0).getTextContent();
        }

        return "";
    }
}
