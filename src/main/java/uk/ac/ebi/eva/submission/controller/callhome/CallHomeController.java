package uk.ac.ebi.eva.submission.controller.callhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.service.CallHomeService;

@RestController
@RequestMapping("/v1/call-home")
public class CallHomeController {
    private final Logger logger = LoggerFactory.getLogger(CallHomeController.class);
    private final CallHomeService callHomeService;

    public CallHomeController(CallHomeService callHomeService) {
        this.callHomeService = callHomeService;
    }

    @PostMapping("/events")
    public ResponseEntity<?> ingest(@RequestBody JsonNode callHomeEventJson) {
        try {
            boolean valid = callHomeService.validateJson(callHomeEventJson);
            if (valid) {
                callHomeService.registerCallHomeEvent(callHomeEventJson);
                return ResponseEntity.ok().build();
            } else {
                return new ResponseEntity<>("Could not register event as the event json is invalid", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            logger.error("Could not register event as an exception occurred: {}", ex.toString());
            return new ResponseEntity<>("Could not register event as an exception occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}