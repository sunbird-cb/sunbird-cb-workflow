package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.WorkflowServiceV2;

@RestController
@RequestMapping("/v2/workflow")
public class WorkFlowControllerV2 {

	@Autowired
	private WorkflowServiceV2 workflowService;

	@PostMapping("/taxonomy/transition")
	public ResponseEntity<Response> wfTransition(@RequestHeader String userToken, @RequestBody WfRequest wfRequest) {
		Response response = workflowService.workflowTransition(userToken, wfRequest);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(path = "taxonomy/{wfId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> getWfApplication(@RequestHeader String userToken,
													 @PathVariable("wfId") String wfId) {
		Response response = workflowService.getWfApplication(userToken, wfId);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response> wfApplicationSearch(@RequestHeader String userToken,
														@RequestBody SearchCriteria searchCriteria) {
		Response response = workflowService.wfApplicationSearch(userToken, searchCriteria);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
