package org.sunbird.workflow.service;

import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.models.WfStatus;

public interface Workflowservice {

    public Response workflowTransition(String rootOrg, String org, WfRequest wfRequest, String userId);

    public Response getWfApplication(String rootOrg, String org, String wfId, String applicationId);

    public Response wfApplicationSearch(String rootOrg, String org, SearchCriteria criteria);

    public Response getApplicationHistoryOnWfId(String rootOrg, String wfId, String userId);

    public Response getNextActionForState(String rootOrg, String org, String serviceName, String state);

    public WfStatus getWorkflowStates(String rootOrg, String org, String serviceName, String state);

    public Response getApplicationWfHistory(String rootOrg, String applicationId);

    public Response applicationsSearch(String rootOrg, String org, SearchCriteria criteria);

    public Response getWorkflowProcess(String rootOrg, String wfId);

    public Response updateUserProfileWF(String rootOrg, String org, WfRequest wfRequest);
    
    public Response getUserWf(String rootOrg, String org, String wid, SearchCriteria criteria);
    
    public Response getUserWFApplicationFields(String rootOrg, String org, String wid, SearchCriteria criteria);
}
