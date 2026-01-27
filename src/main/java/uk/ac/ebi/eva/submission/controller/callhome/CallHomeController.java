package uk.ac.ebi.eva.submission.controller.callhome;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.eva.submission.service.CallHomeService;

@RestController
@RequestMapping("/v1/call-home")
public class CallHomeController {
    private final CallHomeService callHomeService;

    public CallHomeController(CallHomeService callHomeService) {
        this.callHomeService = callHomeService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> ingest(@RequestBody JsonNode callHomeEventJson) {
        callHomeService.registerCallHomeEvent(callHomeEventJson);
        return ResponseEntity.ok().build();
    }
}