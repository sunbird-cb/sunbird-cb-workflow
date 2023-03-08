package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.WorkflowServiceV2;

@RestController
@RequestMapping("/v2/workflow")
public class WorkFlowControllerV2 {

    @Autowired
    private WorkflowServiceV2 workflowService;

    @PostMapping("/taxonomy/transition")
    public ResponseEntity<?> wfTransition(@RequestHeader String userToken, @RequestBody WfRequest wfRequest) {
        SBApiResponse response = workflowService.workflowTransition(userToken, wfRequest);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping(path = "/taxonomy/{wfId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getWfApplication(@RequestHeader String userToken, @PathVariable("wfId") String wfId) {
        SBApiResponse response = workflowService.getWfApplication(userToken, wfId);
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @PostMapping(path = "/taxonomy/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> wfApplicationSearch(@RequestHeader String userToken, @RequestBody SearchCriteria searchCriteria) {
        SBApiResponse response = workflowService.wfApplicationSearch(userToken, searchCriteria);
        return new ResponseEntity<>(response, response.getResponseCode());
    }
}
