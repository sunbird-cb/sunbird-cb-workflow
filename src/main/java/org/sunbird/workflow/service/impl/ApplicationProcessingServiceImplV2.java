package org.sunbird.workflow.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.WfServiceHandler;

public class ApplicationProcessingServiceImplV2 {

	@Autowired
	WfServiceHandler taxonomyService;

	public void processWfApplicationRequest(WfRequest wfRequest) {
		switch (wfRequest.getServiceName()) {
			case Constants.TAXONOMY_SERVICE_NAME:
			taxonomyService.processMessage(wfRequest);
		default:
			break;
		}
	}
}
