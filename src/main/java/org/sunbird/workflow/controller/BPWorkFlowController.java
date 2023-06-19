package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.BPWorkFlowService;
import org.sunbird.workflow.service.DomainWhiteListWorkFlowService;

@RestController
@RequestMapping("/v1/blendedprogram/workflow")
public class BPWorkFlowController {

    @Autowired
    private BPWorkFlowService bPWorkFlowService;

    @PostMapping("/enrol")
    public ResponseEntity<Response> blendedProgramEnrolWf(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = bPWorkFlowService.enrolBPWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Response> blendedProgramWfUpdate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = bPWorkFlowService.updateBPWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(path = "/read/{wfId}/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getDomainWfApplication(@RequestHeader String rootOrg, @RequestHeader String org,
                                                     @PathVariable("wfId") String wfId, @PathVariable("applicationId") String applicationId) {
        Response response = bPWorkFlowService.readBPWFApplication(rootOrg, org, wfId, applicationId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> domainWfSearch(@RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        System.out.println("In controller");
        Response response = bPWorkFlowService.blendedProgramSearch(rootOrg, org, searchCriteria);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
