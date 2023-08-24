package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.PositionWorkFlowService;

@RestController
@RequestMapping("/v1/position/workflow")
public class PositionWorkFlowController {

    @Autowired
    private PositionWorkFlowService signUpWorkFlowService;

    @PostMapping("/create")
    public ResponseEntity<Response> positionWfCreate(@RequestHeader String rootOrg, @RequestHeader String org,
                                      @RequestBody WfRequest wfRequest) {
        Response response = signUpWorkFlowService.createPositionWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Response> positionWfUpdate(@RequestHeader String rootOrg, @RequestHeader String org,
                                          @RequestBody WfRequest wfRequest) {
        Response response = signUpWorkFlowService.updatePositionWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(path = "/read/{wfId}/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getPositionWfApplication(@RequestHeader String rootOrg, @RequestHeader String org,
                                              @PathVariable("wfId") String wfId, @PathVariable("applicationId") String applicationId) {
        Response response = signUpWorkFlowService.readPositionWFApplication(rootOrg, org, wfId, applicationId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> positionWfSearch(@RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        Response response = signUpWorkFlowService.positionSearch(rootOrg, org, searchCriteria);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
