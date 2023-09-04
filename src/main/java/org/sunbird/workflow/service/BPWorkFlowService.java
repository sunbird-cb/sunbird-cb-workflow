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

    public Response readBPWFApplication(String wfId, boolean isPc);

    /**
     * @param rootOrg   - Root Organization Name ex: "igot"
     * @param org       - Organization name ex: "dopt"
     * @param wfRequest - WorkFlow request which needs to be processed.
     * @return - Return the response of success/failure after processing the request.
     */
    Response adminEnrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    /**
     * This method is responsible for processing the wfRequest based on the state of the wfRequest
     *
     * @param wfRequest - Recieves a wfRequest with the request params.
     */
    public void processWFRequest(WfRequest wfRequest);

    public Response removeBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    /**
     * This method is responsible for removing a user enrollment details
     *
     * @param wfRequest - Receives a wfRequest with the request params.
     */
    public void removeEnrolmentDetails(WfRequest wfRequest);

}
