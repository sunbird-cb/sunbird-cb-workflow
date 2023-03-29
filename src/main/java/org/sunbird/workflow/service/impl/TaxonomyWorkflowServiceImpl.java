package org.sunbird.workflow.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.SunbirdApiRespParam;
import org.sunbird.workflow.models.WfAction;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.models.V2.WfStatusV2;
import org.sunbird.workflow.models.V2.WorkFlowModelV2;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
import org.sunbird.workflow.postgres.repo.WfStatusRepoV2;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.TaxonomyWorkflowService;
import org.sunbird.workflow.util.RequestInterceptor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TaxonomyWorkflowServiceImpl implements TaxonomyWorkflowService {

    @Autowired
    private WfStatusRepoV2 wfStatusRepoV2;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Configuration configuration;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

    @Autowired
    private Producer producer;

    @Autowired
    RequestInterceptor requestInterceptor;
    Logger log = LogManager.getLogger(TaxonomyWorkflowServiceImpl.class);

    /**
     * create the workflow application
     * @param userToken
     * @param wfRequest
     * @return
     */
    @Override
    public SBApiResponse createWorkflow(String userToken, WfRequest wfRequest) {
        SBApiResponse response = createDefaultResponse(Constants.TAXONOMY_WORKFLOW_CREATE);
        String userId = requestInterceptor.fetchUserIdFromAccessToken(userToken);
        String errMsg = validateCreateWfRequest(wfRequest, userId);
        if (!StringUtils.isEmpty(errMsg)) {
            response.getParams().setErrmsg(errMsg);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        response = changeStatus(wfRequest, userId, response);
        errMsg = (String) response.getResult().get(Constants.ERROR_MESSAGE);
        if (StringUtils.isNotEmpty(errMsg)) {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(errMsg);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
        } else {
            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
        }
        return response;
    }

    /**
     * update the workflow application
     * @param userToken
     * @param wfRequest
     * @return
     */
    @Override
    public SBApiResponse updateWorkflow(String userToken, WfRequest wfRequest) {
        SBApiResponse response = createDefaultResponse(Constants.TAXONOMY_WORKFLOW_UPDATE);
        String userId = requestInterceptor.fetchUserIdFromAccessToken(userToken);
        String errMsg = validateUpdateWfRequest(wfRequest, userId);
        if (!StringUtils.isEmpty(errMsg)) {
            response.getParams().setErrmsg(errMsg);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        response = changeStatus(wfRequest, userId, response);
        errMsg = (String) response.getResult().get(Constants.ERROR_MESSAGE);
        if (StringUtils.isNotEmpty(errMsg)) {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(errMsg);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
        } else {
            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
        }
        return response;
    }

    /**
     * Get the application based on wfId
     *
     * @param userToken
     * @param wfId
     * @return Wf Application based on wfId
     */
    @Override
    public SBApiResponse getWfApplication(String userToken, String wfId) {
        SBApiResponse response = createDefaultResponse(Constants.GET_WF_APPLICATION);
        String userId = requestInterceptor.fetchUserIdFromAccessToken(userToken);
        WfStatusEntityV2 applicationStatus = wfStatusRepoV2.findByUserIdAndWfId(userId, wfId);
        List<Map<String, Object>> updateFieldValuesList = null;
        String errMsg = null;
        if (!ObjectUtils.isEmpty(applicationStatus)) {
            if (StringUtils.isNotEmpty(applicationStatus.getUpdateFieldValues())) {
                try {
                    updateFieldValuesList = mapper.readValue(applicationStatus.getUpdateFieldValues(), new TypeReference<List<Map<String, Object>>>() {
                    });
                } catch (Exception ex) {
                    log.error("Exception occurred while parsing wf fields!");
                    errMsg = Constants.UPDATED_FIELD_VALUES_ERROR_MESSAGE;
                }
            }
        } else {
            errMsg = Constants.WF_NOT_AVAILABLE;
        }
        if (StringUtils.isNotBlank(errMsg) ) {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(errMsg);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Map<String, Object> wfMap = new HashMap<>();
        wfMap.put(Constants.WF_ID_CONSTANT, applicationStatus.getWfId());
        wfMap.put(Constants.USER_ID, applicationStatus.getUserId());
        wfMap.put(Constants.CURRENT_STATUS, applicationStatus.getCurrentStatus());
        wfMap.put(Constants.IS_WORKFLOW_CONST, applicationStatus.getInWorkflow());
        wfMap.put(Constants.SERVICE_NAME, applicationStatus.getServiceName());
        wfMap.put(Constants.CREATED_ON, applicationStatus.getCreatedOn());
        wfMap.put(Constants.CREATED_BY, applicationStatus.getCreatedBy());
        wfMap.put(Constants.LAST_UPDATED_ON, applicationStatus.getLastUpdatedOn());
        wfMap.put(Constants.UPDATED_FIELDS_VALUES, updateFieldValuesList);

        response.getResult().putAll(wfMap);
        response.getParams().setStatus(Constants.SUCCESSFUL);
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    /**
     * Get workflow applications based on status
     *
     * @param userToken
     * @param criteria
     * @return workflow applications
     */
    @Override
    public SBApiResponse wfApplicationSearch(String userToken, SearchCriteria criteria) {
        //user token need to implement
        SBApiResponse response = createDefaultResponse(Constants.TAXONOMY_WF_APPLICATION_SEARCH);
        if (criteria.isEmpty()) {
            response.getParams().setErrmsg(Constants.SEARCH_CRITERIA_VALIDATION);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        Pageable pageable = getPageReqForApplicationSearch(criteria);
        Page<WfStatusEntityV2> statePage = wfStatusRepoV2.findByServiceNameAndCurrentStatus
                (criteria.getServiceName(), criteria.getApplicationStatus(), pageable);
        response.put(Constants.DATA, statePage.getContent());
        response.getParams().setStatus(Constants.SUCCESSFUL);
        response.setResponseCode(HttpStatus.OK);
        return response;
    }

    private Pageable getPageReqForApplicationSearch(SearchCriteria criteria) {
        Pageable pageable;
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
        pageable = PageRequest.of(offset, limit + offset);
        return pageable;
    }

    private SBApiResponse changeStatus(WfRequest wfRequest, String userId, SBApiResponse response) {
        String wfId = wfRequest.getWfId();
        String nextState = null;
        String errMsg = null;
        try {
            WorkFlowModelV2 workFlowModel = getWorkFlowConfig(wfRequest.getServiceName());
            WfStatusV2 wfStatus = getWfStatus(wfRequest.getState(), workFlowModel);
            if (wfStatus == null) {
                response.getParams().setErrmsg(Constants.WORKFLOW_STATE_CHECK_ERROR);
                return response;
            }

            WfAction wfAction = getWfAction(wfRequest.getAction(), wfStatus);
            if (wfAction == null) {
                response.getParams().setErrmsg(Constants.WORKFLOW_ACTION_ERROR);
                return response;
            }
            nextState = wfAction.getNextState();

            WfStatusV2 wfNextStatus = getWfStatus(nextState, workFlowModel);
            if (wfNextStatus == null) {
                response.getParams().setErrmsg(Constants.WORKFLOW_STATE_CHECK_ERROR);
                return response;
            }

            WfStatusEntityV2 applicationStatus = wfStatusRepoV2.findByUserIdAndWfId(userId, wfRequest.getWfId());
            errMsg = validateUserAndWfStatus(wfRequest, wfStatus, applicationStatus);
            if (StringUtils.isEmpty(errMsg)) {
                // actor has proper role to take the workflow action
                if (ObjectUtils.isEmpty(applicationStatus)) {
                    applicationStatus = new WfStatusEntityV2();
                    wfId = UUID.randomUUID().toString();
                    applicationStatus.setWfId(wfId);
                    applicationStatus.setServiceName(wfRequest.getServiceName());
                    applicationStatus.setUserId(userId);
                    applicationStatus.setCreatedBy(userId);
                    applicationStatus.setCreatedOn(new Date());
                    applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
                    wfRequest.setWfId(wfId);
                }

                applicationStatus.setLastUpdatedOn(new Date());
                applicationStatus.setCurrentStatus(nextState);
                applicationStatus.setInWorkflow(!wfNextStatus.getIsLastState());

                wfStatusRepoV2.save(applicationStatus);
                if (wfStatus.getNotificationEnable() == Boolean.TRUE) {
                    producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
                }
                producer.push(configuration.getTaxonomyWorkflowKafkaTopic(), wfRequest);
            }
        } catch (IOException e) {
            errMsg = Constants.WORKFLOW_PARSING_ERROR_MESSAGE;
            log.error(Constants.WORKFLOW_PARSING_ERROR_MESSAGE);
        }
        response.getParams().setErrmsg(errMsg);
        HashMap<String, Object> data = new HashMap<>();
        data.put(Constants.WF_ID_CONSTANT, wfId);
        data.put(Constants.STATUS, nextState);
        data.put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + nextState);
        response.getResult().putAll(data);
        return response;
    }

    private String validateCreateWfRequest(WfRequest wfRequest, String userId) {
        List<String> params = new ArrayList<String>();
        StringBuilder strBuilder = new StringBuilder();
        if (StringUtils.isEmpty(userId)) {
            params.add(Constants.USER_ID_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(wfRequest.getState())) {
            params.add(Constants.STATE_VALIDATION_ERROR);
        }
        if (StringUtils.isNotEmpty(wfRequest.getWfId())) {
            params.add(Constants.WFID_VALIDATION_ERROR_FOR_INITIATE);
        }
        if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
            params.add(Constants.FIELD_VALUE_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(wfRequest.getAction())) {
            params.add(Constants.ACTION_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(wfRequest.getServiceName())) {
            params.add(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
        }
        if (!params.isEmpty()) {
            strBuilder.append("Invalid Request. " + params);
        }
        return strBuilder.toString();
    }

        /**
         * Validate the workflow request
         *
         * @param wfRequest
         */
        private String validateUpdateWfRequest(WfRequest wfRequest, String userId) {
            List<String> params = new ArrayList<String>();
            StringBuilder strBuilder = new StringBuilder();
            if (StringUtils.isEmpty(userId)) {
                params.add(Constants.USER_ID_VALIDATION_ERROR);
            }
            if (StringUtils.isEmpty(wfRequest.getState())) {
                params.add(Constants.STATE_VALIDATION_ERROR);
            }
            if (StringUtils.isEmpty(wfRequest.getWfId())) {
                params.add(Constants.WFID_VALIDATION_ERROR_FOR_REVIEW);
            }
            if (CollectionUtils.isNotEmpty(wfRequest.getUpdateFieldValues())) {
                params.add(Constants.FIELD_VALUE_VALIDATION_ERROR_FOR_UPDATE);
            }
            if (StringUtils.isEmpty(wfRequest.getAction())) {
                params.add(Constants.ACTION_VALIDATION_ERROR);
            }
            if (StringUtils.isEmpty(wfRequest.getServiceName())) {
                params.add(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
            }
            if (!params.isEmpty()) {
                strBuilder.append("Invalid Request. " + params);
            }
            return strBuilder.toString();
        }

    /**
     * Validate application against workflow state
     *
     * @param wfRequest
     * @param wfStatus
     * @param applicationStatus
     */
    private String validateUserAndWfStatus(WfRequest wfRequest, WfStatusV2 wfStatus, WfStatusEntityV2 applicationStatus) {
        List<String> params = new ArrayList<String>();
        StringBuilder strBuilder = new StringBuilder();
        if (StringUtils.isEmpty(wfRequest.getWfId()) && !wfStatus.getStartState()) {
            params.add(Constants.WORKFLOW_ID_ERROR_MESSAGE);
        }

        if ((!ObjectUtils.isEmpty(applicationStatus))
                && (!wfRequest.getState().equalsIgnoreCase(applicationStatus.getCurrentStatus()))) {
            params.add("Application is in " + applicationStatus.getCurrentStatus()
                    + " State but trying to be move in " + wfRequest.getState() + " state!");
        }
        if (!params.isEmpty()) {
            strBuilder.append(params);
        }
        return strBuilder.toString();
    }

    /**
     * Get Workflow Action based on given action
     *
     * @param action
     * @param wfStatus
     * @return Work flow Action
     */
    private WfAction getWfAction(String action, WfStatusV2 wfStatus) {
        WfAction wfAction = null;
        if (ObjectUtils.isEmpty(wfStatus.getActions())) {
            return null;
        }
        for (WfAction filterAction : wfStatus.getActions()) {
            if (action.equals(filterAction.getAction())) {
                wfAction = filterAction;
            }
        }
        return wfAction;
    }

    /**
     * Get Workflow configuration details
     *
     * @param serviceName
     * @return Work flow model
     */
    private WorkFlowModelV2 getWorkFlowConfig(String serviceName) {
        WorkFlowModelV2 workFlowModel = new WorkFlowModelV2();
        try {
            Map<String, Object> wfConfig = new HashMap<>();
            if (serviceName.equalsIgnoreCase(Constants.TAXONOMY_SERVICE_NAME)) {
                StringBuilder uri = new StringBuilder();
                uri.append(configuration.getLmsServiceHost() + configuration.getTaxonomyServiceConfigPath());
                wfConfig = (Map<String, Object>) requestServiceImpl.fetchResultUsingGet(uri);
            }
            Map<String, Object> result = (Map<String, Object>) wfConfig.get(Constants.RESULT);
            Map<String, Object> response = (Map<String, Object>) result.get(Constants.RESPONSE);
            Map<String, Object> wfStates = mapper.readValue((String) response.get(Constants.VALUE), Map.class);
            workFlowModel = mapper.convertValue(wfStates, new TypeReference<WorkFlowModelV2>() {
            });
        } catch (Exception e) {
            log.error("Exception occurred while getting work flow config details!");
        }
        return workFlowModel;
    }

    /**
     * Get the workflow State based on given state
     *
     * @param state
     * @param workFlowModel
     * @return Workflow State
     */
    private WfStatusV2 getWfStatus(String state, WorkFlowModelV2 workFlowModel) {
        WfStatusV2 wfStatus = null;
        for (WfStatusV2 status : workFlowModel.getWfstates()) {
            if (status.getState().equals(state)) {
                wfStatus = status;
            }
        }
        return wfStatus;
    }

    private SBApiResponse createDefaultResponse(String api) {
        SBApiResponse response = new SBApiResponse();
        response.setId(api);
        response.setVer(Constants.API_VERSION_1);
        response.setParams(new SunbirdApiRespParam());
        response.getParams().setStatus(Constants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(LocalDateTime.now().toString());
        return response;
    }
}
