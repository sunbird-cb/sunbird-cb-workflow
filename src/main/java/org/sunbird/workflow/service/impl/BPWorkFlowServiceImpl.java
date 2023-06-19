package org.sunbird.workflow.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.BPWorkFlowService;
import org.sunbird.workflow.service.Workflowservice;

@Service
public class BPWorkFlowServiceImpl implements BPWorkFlowService {

    @Autowired
    private Workflowservice workflowService;

    @Override
    public Response enrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response updateBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response readBPWFApplication(String rootOrg, String org, String wfId, String applicationId) {
        Response response = workflowService.getWfApplication(rootOrg, org, wfId, applicationId);
        return response;
    }

    @Override
    public Response blendedProgramSearch(String rootOrg, String org, SearchCriteria criteria) {
        Response response = workflowService.applicationsSearch(rootOrg, org, criteria, Constants.BLENDED_PROGRAM_SEARCH_ENABLED);
        return response;
    }
}
