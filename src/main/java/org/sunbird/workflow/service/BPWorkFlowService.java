package org.sunbird.workflow.service;

import java.util.Map;

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

     * This method is responsible for processing the wfRequest based on the state of the wfRequest
     *
     * @param wfRequest - Recieves a wfRequest with the request params.
     */
    public void processWFRequest(WfRequest wfRequest);

    /**
     * Retrieves the statistics of user count in various state for the given courseIds
     * @param request - Request object which contanins the course id list
     * @return - returns the statistics information of user count for each course and batch.
     */
    public Response readStats(Map<String, Object> request);
     /*
     * @param rootOrg   - Root Organization Name ex: "igot"
     * @param org       - Organization name ex: "dopt"
     * @param wfRequest - WorkFlow request which needs to be processed.
     * @return - Return the response of success/failure after processing the request.
     */
    Response adminEnrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    public Response removeBPWorkFlow(String rootOrg, String org, WfRequest wfRequest);


}
