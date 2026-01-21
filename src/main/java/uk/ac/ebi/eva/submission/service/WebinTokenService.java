package uk.ac.ebi.eva.submission.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.eva.submission.entity.SubmissionAccount;
import uk.ac.ebi.eva.submission.model.WebinSubmissionContact;
import uk.ac.ebi.eva.submission.model.WebinUserInfo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WebinTokenService {
    private final Logger logger = LoggerFactory.getLogger(WebinTokenService.class);

    @Value("${webin.userinfo.url}")
    private String userInfoUrl;

    public SubmissionAccount getWebinUserAccountFromToken(String userToken) {
        logger.debug("Attempting Webin token validation");
        try {
            String restJsonResponse = TokenServiceUtil.getUserInfoRestResponse(userToken, this.userInfoUrl);
            if (restJsonResponse == null) {
                logger.warn("Webin token validation failed: no response from user info endpoint");
                return null;
            }
            return createWebinUserAccount(restJsonResponse);
        } catch (Exception e) {
            logger.warn("Webin token validation failed: {}", e.getMessage());
            return null;
        }
    }

    public SubmissionAccount createWebinUserAccount(String jsonResponse) {
        // convert json response to WebinUserInfo object
        WebinUserInfo webinUserInfo = new Gson().fromJson(jsonResponse, WebinUserInfo.class);

        String accountId = webinUserInfo.getSubmissionAccountId();

        WebinSubmissionContact mainContact;
        // Get main contact, if not present take the first one
        Optional<WebinSubmissionContact> opMainContact = webinUserInfo.getSubmissionContacts().stream()
                .filter(sc -> sc.getMainContact()).findAny();
        if (opMainContact.isPresent()) {
            mainContact = opMainContact.get();
        } else {
            mainContact = webinUserInfo.getSubmissionContacts().get(0);
        }

        // get all the email ids except the main contact
        List<String> secondaryEmails = webinUserInfo.getSubmissionContacts().stream()
                .map(sc -> sc.getEmailAddress())
                .filter(email -> !email.equals(mainContact.getEmailAddress()))
                .collect(Collectors.toList());

        String firstNameOrConsortiumName = mainContact.getFirstName() != null ? mainContact.getFirstName() : mainContact.getConsortium();
        String lastName = mainContact.getSurname() != null ? mainContact.getSurname() : "";
        return new SubmissionAccount(accountId, LoginMethod.WEBIN.getLoginType(), firstNameOrConsortiumName,
                lastName, mainContact.getEmailAddress(), secondaryEmails);
    }
}
