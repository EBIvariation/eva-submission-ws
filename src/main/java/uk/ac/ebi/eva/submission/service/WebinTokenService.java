package uk.ac.ebi.eva.submission.service;

import com.google.gson.Gson;
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

    @Value("${webin.userinfo.url}")
    private String userInfoUrl;

    public SubmissionAccount getWebinUserAccountFromToken(String userToken) {
        String restJsonResponse = TokenServiceUtil.getUserInfoRestResponse(userToken, this.userInfoUrl);
        return createWebinUserAccount(restJsonResponse);
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

        return new SubmissionAccount(accountId, LoginMethod.WEBIN.getLoginType(), mainContact.getFirstName(),
                mainContact.getSurname(), mainContact.getEmailAddress(), secondaryEmails);
    }
}
