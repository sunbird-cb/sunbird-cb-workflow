package org.sunbird.workflow.service;

import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;

public interface BPWorkFlowService {

    public Response enrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    public Response updateBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    public Response readBPWFApplication(String rootOrg, String org, String wfId, String applicationId);

    public Response blendedProgramSearch(String rootOrg, String org, SearchCriteria criteria);

    public void updateEnrolmentDetails(WfRequest wfRequest);

    Response blendedProgramUserSearch(String rootOrg, String org, String userId, SearchCriteria searchCriteria);
}
