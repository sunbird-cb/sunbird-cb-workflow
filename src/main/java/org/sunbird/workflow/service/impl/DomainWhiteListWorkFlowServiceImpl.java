package org.sunbird.workflow.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfDomainLookup;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfDomainLookupRepo;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.DomainWhiteListWorkFlowService;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.Workflowservice;
import org.sunbird.workflow.utils.CassandraOperation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DomainWhiteListWorkFlowServiceImpl implements DomainWhiteListWorkFlowService {

    @Autowired
    private Workflowservice workflowService;

    @Autowired
    private Configuration configuration;

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    private UserProfileWfService userProfileWfService;

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    private WfDomainLookupRepo wfDomainLookupRepo;

    @Override
    public Response createDomainWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = new Response();
        HashMap<String, Object> updatedFieldValue = wfRequest.getUpdateFieldValues().stream().findFirst().get();
        HashMap<String, String> toValue = (HashMap<String, String>) updatedFieldValue.get("toValue");
        String domainValue =  toValue.get("domain").isEmpty() ? "" : toValue.get("domain");
        List<WfDomainLookup> domainLookup = wfDomainLookupRepo.findByDomainName(domainValue);
        if(CollectionUtils.isNotEmpty(domainLookup)) {
            response.put(Constants.ERROR_MESSAGE, Constants.DOMAIN_NAME_REQUEST_ALREADY_ERROR_MSG + ": " + domainValue);
            response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }
        response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response updateDomainWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response readDomainWFApplication(String rootOrg, String org, String wfId, String applicationId) {
        Response response = workflowService.getWfApplication(rootOrg, org, wfId, applicationId);
        return response;
    }

    @Override
    public Response domainSearch(String rootOrg, String org, SearchCriteria criteria) {
        Response response = applicationSearchOnApplicationIdGroup(criteria, rootOrg);
        return response;
    }

    public Response applicationSearchOnApplicationIdGroup(SearchCriteria criteria, String rootOrg) {
        Pageable pageable = getPageReqForApplicationSearch(criteria);
        List<String> applicationIds = criteria.getApplicationIds();
        Map<String, List<WfStatusEntity>> infos = null;
        long totalDomainRequestCount = 0;
        if (CollectionUtils.isEmpty(applicationIds)) {
            Page<String> applicationIdsPage = wfStatusRepo.getListOfDistinctApplicationUsingDept(criteria.getServiceName(),
                    criteria.getApplicationStatus(), criteria.getDeptName(), pageable);
            applicationIds = applicationIdsPage.getContent();
            totalDomainRequestCount = applicationIdsPage.getTotalElements();
        }
        List<WfStatusEntity> wfStatusEntities = null;
        wfStatusEntities = wfStatusRepo.findByServiceNameAndCurrentStatusAndDeptNameAndApplicationIdIn(
                criteria.getServiceName(), criteria.getApplicationStatus(), criteria.getDeptName(), applicationIds);
        infos = wfStatusEntities.stream().collect(Collectors.groupingBy(WfStatusEntity::getApplicationId));
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, infos);
        response.put(Constants.STATUS, HttpStatus.OK);
        response = getResponse(rootOrg, response);
        response.put(Constants.COUNT, totalDomainRequestCount);
        return response;
    }

    private Pageable getPageReqForApplicationSearch(SearchCriteria criteria) {
        Pageable pageable;
        if (criteria.isEmpty()) {
            throw new BadRequestException(Constants.SEARCH_CRITERIA_VALIDATION);
        }
        Integer limit = configuration.getDefaultLimit();
        Integer offset = configuration.getDefaultOffset();
        if (criteria.getLimit() == null && criteria.getOffset() == null)
            limit = configuration.getMaxLimit();
        if (criteria.getLimit() != null && criteria.getLimit() <= configuration.getDefaultLimit())
            limit = criteria.getLimit();
        if (criteria.getLimit() != null && criteria.getLimit() > configuration.getDefaultLimit())
            limit = configuration.getDefaultLimit();
        if (criteria.getOffset() != null)
            offset = criteria.getOffset();
        pageable = PageRequest.of(offset, limit);
        return pageable;
    }

    private Response getResponse(String rootOrg, Response wfApplicationSearchResponse) {
        Response response;
        List<Map<String, Object>> userProfiles = userProfileWfService.enrichUserData(
                (Map<String, List<WfStatusEntity>>) wfApplicationSearchResponse.get(Constants.DATA), rootOrg);
        response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, userProfiles);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    @Override
    public void processDomainRequest(WfRequest wfRequest) {
        if (Constants.APPROVE_STATE.equalsIgnoreCase(wfRequest.getAction())) {
            List<HashMap<String, Object>> updatedFieldValues = wfRequest.getUpdateFieldValues();
            String approvedDomain = "";
            for (HashMap<String, Object> updatedFieldValue : updatedFieldValues) {
                if (updatedFieldValue.containsKey(Constants.TO_VALUE)) {
                    Map<String, Object> toValueMap = (Map<String, Object>) updatedFieldValue.get(Constants.TO_VALUE);
                    approvedDomain = (String) toValueMap.get(Constants.DOMAIN);
                }
            }
            if (StringUtils.isNotEmpty(approvedDomain) && !isAlreadyApprovedDomains(approvedDomain)) {
                Map<String, Object> propertyMap = new HashMap<>();
                propertyMap.put(Constants.CONTEXT_TYPE, Constants.USER_REGISTRATION_PRE_APPROVED_DOMAIN);
                propertyMap.put(Constants.CONTEXT_NAME, approvedDomain);
                cassandraOperation.insertRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_MASTER_DATA, propertyMap);
            }
        }
    }

    private Boolean isAlreadyApprovedDomains(String emailDomain) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.CONTEXT_TYPE, Constants.USER_REGISTRATION_PRE_APPROVED_DOMAIN);
        propertyMap.put(Constants.CONTEXT_NAME, emailDomain);
        List<Map<String, Object>> listOfDomains = cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD, Constants.TABLE_MASTER_DATA, propertyMap, Arrays.asList(Constants.CONTEXT_TYPE, Constants.CONTEXT_NAME));
        return CollectionUtils.isNotEmpty(listOfDomains);
    }
}
