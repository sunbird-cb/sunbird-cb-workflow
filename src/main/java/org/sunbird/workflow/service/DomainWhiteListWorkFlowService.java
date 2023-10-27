package org.sunbird.workflow.service;

import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;

public interface DomainWhiteListWorkFlowService {

    public Response createDomainWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    public Response updateDomainWorkFlow(String rootOrg, String org, WfRequest wfRequest);

    public Response readDomainWFApplication(String rootOrg, String org, String wfId, String applicationId);

    public Response domainSearch(String rootOrg, String org, SearchCriteria criteria);

    public void  processDomainRequest(WfRequest wfRequest);
}
