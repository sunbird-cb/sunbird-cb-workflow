package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.OrganisationWorkFlowService;

@RestController
@RequestMapping("/v1/org/workflow")
public class OrganisationWorkFlowController {

    @Autowired
    private OrganisationWorkFlowService organisationWorkFlowService;

    @PostMapping("/create")
    public ResponseEntity<Response> orgWfCreate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = organisationWorkFlowService.createOrgWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Response> orgWfUpdate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = organisationWorkFlowService.updateOrgWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(path = "/read/{wfId}/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getOrgWfApplication(@RequestHeader String rootOrg, @RequestHeader String org,
                                                     @PathVariable("wfId") String wfId, @PathVariable("applicationId") String applicationId) {
        Response response = organisationWorkFlowService.readOrgWFApplication(rootOrg, org, wfId, applicationId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> orgWfSearch(@RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        System.out.println("In controller");
        Response response = organisationWorkFlowService.orgSearch(rootOrg, org, searchCriteria);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
