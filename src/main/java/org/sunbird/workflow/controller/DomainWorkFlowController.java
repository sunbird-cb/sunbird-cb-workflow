package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.DomainWhiteListWorkFlowService;

@RestController
@RequestMapping("/v1/domain/workflow")
public class DomainWorkFlowController {

    @Autowired
    private DomainWhiteListWorkFlowService domainWhiteListWorkFlowService;

    @PostMapping("/create")
    public ResponseEntity<Response> domainWfCreate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = domainWhiteListWorkFlowService.createDomainWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<Response> domainWfUpdate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = domainWhiteListWorkFlowService.updateDomainWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(path = "/read/{wfId}/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getDomainWfApplication(@RequestHeader String rootOrg, @RequestHeader String org,
                                                     @PathVariable("wfId") String wfId, @PathVariable("applicationId") String applicationId) {
        Response response = domainWhiteListWorkFlowService.readDomainWFApplication(rootOrg, org, wfId, applicationId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> domainWfSearch(@RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        System.out.println("In controller");
        Response response = domainWhiteListWorkFlowService.domainSearch(rootOrg, org, searchCriteria);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
