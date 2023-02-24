package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.Workflowservice;

@RestController
@RequestMapping("/v1/workflow/taxonomy")
public class TaxonomyWorkFlowController {
    @Autowired
    private Workflowservice workflowService;
    @PostMapping("/transition")
    public ResponseEntity<Response> wfTransition(@RequestHeader String rootOrg, @RequestHeader String org,
                                                 @RequestBody WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
