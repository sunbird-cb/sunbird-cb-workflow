package org.sunbird.workflow.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.BPWorkFlowService;

import org.sunbird.workflow.service.DomainWhiteListWorkFlowService;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/blendedprogram/workflow")
public class BPWorkFlowController {

    @Autowired
    private BPWorkFlowService bPWorkFlowService;

    @PostMapping("/enrol")
    public ResponseEntity<Response> blendedProgramEnrolWf(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = bPWorkFlowService.enrolBPWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @PostMapping("/update")
    public ResponseEntity<Response> blendedProgramWfUpdate(@RequestHeader String rootOrg, @RequestHeader String org,
                                             @RequestBody WfRequest wfRequest) {
        Response response = bPWorkFlowService.updateBPWorkFlow(rootOrg, org, wfRequest);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @GetMapping(path = "/read/{wfId}/{applicationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getBlendedProgramWfApplication(@RequestHeader String rootOrg, @RequestHeader String org,
                                                     @PathVariable("wfId") String wfId, @PathVariable("applicationId") String applicationId) {
        Response response = bPWorkFlowService.readBPWFApplication(rootOrg, org, wfId, applicationId);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> blendedProgramWfSearch(@RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        //Department is not eligible filter for the Blended Program Search, marking it as null.
        if(searchCriteria !=null) searchCriteria.setDeptName(null);
        Response response = bPWorkFlowService.blendedProgramSearch(rootOrg, org, searchCriteria);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @PostMapping(path = "/user/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> userWfSearch(@RequestHeader(Constants.X_AUTH_USER_ID) String userId, @RequestHeader String rootOrg, @RequestHeader String org, @RequestBody SearchCriteria searchCriteria) {
        Response response = bPWorkFlowService.blendedProgramUserSearch(rootOrg, org, userId, searchCriteria);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @GetMapping(path = "/read/mdo/{wfId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getBPApplicationByMdo(
                                                     @PathVariable("wfId") String wfId) {
        Response response = bPWorkFlowService.readBPWFApplication(wfId, false);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }

    @GetMapping(path = "/read/pc/{wfId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response> getBpApplicationByPC(
                                                     @PathVariable("wfId") String wfId) {
        Response response = bPWorkFlowService.readBPWFApplication(wfId, true);
        return new ResponseEntity<>(response, (HttpStatus) response.get(Constants.STATUS));
    }


    @PostMapping(path = "/stats")
    public ResponseEntity<Response> getBatchStats(@RequestBody Map<String, Object> request) {
        Response response = bPWorkFlowService.readStats(request);
        return new ResponseEntity<Response>(response, response.getResponseCode());


    /**
     * @param rootOrg   - Root Organization Name ex: "igot"
     * @param org       - Organization name ex: "dopt"
     * @param wfRequests - Bulk WorkFlow requests which needs to be processed.
     * @return - Return the response of success/failure after processing the request.
     */
    @PostMapping("/admin/enrol")
    public ResponseEntity<List<Response>> blendedProgramAdminEnrolWf(@RequestHeader String rootOrg, @RequestHeader String org,
                                                                     @RequestBody List<WfRequest> wfRequests)  {
        List<Response> responses = new ArrayList<>();
        for(WfRequest wfRequest:wfRequests){
            Response response = bPWorkFlowService.adminEnrolBPWorkFlow(rootOrg, org, wfRequest);
            responses.add(response);
        }
        return new ResponseEntity<>(responses,HttpStatus.OK);

    }
}
