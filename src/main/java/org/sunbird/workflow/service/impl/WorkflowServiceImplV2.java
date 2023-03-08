package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.sunbird.workflow.models.*;
import org.sunbird.workflow.models.V2.WfStatusV2;
import org.sunbird.workflow.models.V2.WorkFlowModelV2;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
import org.sunbird.workflow.postgres.repo.WfStatusRepoV2;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.WorkflowServiceV2;
import org.sunbird.workflow.util.RequestInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkflowServiceImplV2 implements WorkflowServiceV2 {

    @Autowired
    private WfStatusRepoV2 wfStatusRepoV2;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Configuration configuration;

    @Autowired
    private UserProfileWfService userProfileWfService;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

    @Autowired
    private Producer producer;

    @Autowired
    RequestInterceptor requestInterceptor;
    Logger log = LogManager.getLogger(WorkflowServiceImplV2.class);


    /**
     * Change the status of workflow application
     *
     * @param userToken
     * @param wfRequest
     * @return
     */
    @Override
    public SBApiResponse workflowTransition(String userToken, WfRequest wfRequest) {
        SBApiResponse response = createDefaultResponse(Constants.TAXONOMY_WORKFLOW_TRANSITION);
        String userId = requestInterceptor.fetchUserIdFromAccessToken(userToken);
        String errMsg = validateWfRequest(wfRequest, userId);
        if (!StringUtils.isEmpty(errMsg)) {
            response.getParams().setErrmsg(errMsg);
            response.getParams().setStatus(Constants.FAILED);
            response.setResponseCode(HttpStatus.BAD_REQUEST);
            return response;
        }
        HashMap<String, String> changeStatusResponse = changeStatus(wfRequest, userId);
        if (StringUtils.isNotEmpty(changeStatusResponse.get(Constants.ERROR_MESSAGE))) {
            response.getParams().setStatus(Constants.FAILED);
            response.getParams().setErrmsg(changeStatusResponse.get(Constants.ERROR_MESSAGE));
            response.setResponseCode(HttpStatus.BAD_REQUEST);
        } else {
            HashMap<String, Object> data = new HashMap<>();
            data.put(Constants.STATUS, changeStatusResponse.get(Constants.STATUS));
            data.put(Constants.WF_ID_CONSTANT, changeStatusResponse.get(Constants.WF_ID_CONSTANT));
            response.getParams().setStatus(Constants.SUCCESSFUL);
            response.setResponseCode(HttpStatus.OK);
            response.getResult().put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + changeStatusResponse.get(Constants.STATUS));
            response.getResult().putAll(data);
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

    private HashMap<String, String> changeStatus(WfRequest wfRequest, String userId) {
        String wfId = wfRequest.getWfId();
        String nextState = null;
        String errMsg = null;
        HashMap<String, String> data = new HashMap<>();
        try {
            validateWfRequest(wfRequest, userId);
            WfStatusEntityV2 applicationStatus = wfStatusRepoV2.findByUserIdAndWfId(userId, wfRequest.getWfId());
            WorkFlowModelV2 workFlowModel = getWorkFlowConfig(wfRequest.getServiceName());
            WfStatusV2 wfStatus = getWfStatus(wfRequest.getState(), workFlowModel);
            if (wfStatus == null) {
                data.put(Constants.ERROR_MESSAGE, Constants.WORKFLOW_STATE_CHECK_ERROR);
                return data;
            }
            errMsg = validateUserAndWfStatus(wfRequest, wfStatus, applicationStatus);
            WfAction wfAction = getWfAction(wfRequest.getAction(), wfStatus);
            if (wfAction == null) {
                data.put(Constants.ERROR_MESSAGE, Constants.WORKFLOW_ACTION_ERROR);
                return data;
            }
            // actor has proper role to take the workflow action
            nextState = wfAction.getNextState();
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
            WfStatusV2 wfStatusCheckForNextState = getWfStatus(nextState, workFlowModel);
            if (wfStatusCheckForNextState == null) {
                data.put(Constants.ERROR_MESSAGE, Constants.WORKFLOW_STATE_CHECK_ERROR);
                return data;
            }

            applicationStatus.setLastUpdatedOn(new Date());
            applicationStatus.setCurrentStatus(nextState);
            applicationStatus.setInWorkflow(!wfStatusCheckForNextState.getIsLastState());

            if (StringUtils.isEmpty(errMsg)) {
                wfStatusRepoV2.save(applicationStatus);
                if (wfStatus.getNotificationEnable() == Boolean.TRUE) {
                    producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
                }
                producer.push(configuration.getWorkflowApplicationTopic(), wfRequest);
            }
        } catch (IOException e) {
            errMsg = Constants.WORKFLOW_PARSING_ERROR_MESSAGE;
            log.error(Constants.WORKFLOW_PARSING_ERROR_MESSAGE);
        }
        data.put(Constants.ERROR_MESSAGE, errMsg);
        data.put(Constants.WF_ID_CONSTANT, wfId);
        data.put(Constants.STATUS, nextState);
        return data;
    }

    /**
     * Validate the workflow request
     *
     * @param wfRequest
     */
    private String validateWfRequest(WfRequest wfRequest, String userId) {
        List<String> params = new ArrayList<String>();
        StringBuilder strBuilder = new StringBuilder();
        if (StringUtils.isEmpty(wfRequest.getState())) {
            params.add(Constants.STATE_VALIDATION_ERROR);
        } else {
            if (StringUtils.isEmpty(wfRequest.getWfId())) {
                if (!wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                    params.add(Constants.STATUS_VALIDATION_ERROR_FOR_INITIATE);
                }
            } else {
                if (wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                    params.add(Constants.STATUS_VALIDATION_ERROR_FOR_NOT_INITIATE);
                }
            }
            if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
                if (wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                    params.add(Constants.FIELD_VALUE_VALIDATION_ERROR);
                }
            } else {
                ArrayList<String> invalidTerms = new ArrayList<>();
                for (HashMap<String, Object> updatedField : wfRequest.getUpdateFieldValues()) {
                    if (StringUtils.isEmpty((String) updatedField.get(Constants.APPROVAL_STATUS)) || !updatedField.get(Constants.APPROVAL_STATUS).equals(Constants.DRAFT)) {
                        invalidTerms.add((String) updatedField.get(Constants.IDENTIFIER));
                    }
                }
                if (CollectionUtils.isNotEmpty(invalidTerms)) {
                    params.add(Constants.TERM_APPROVAL_STATUS_ERROR + ": " + invalidTerms);
                }
            }
        }
        if (StringUtils.isEmpty(userId)) {
            params.add(Constants.USER_ID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getAction())) {
            params.add(Constants.ACTION_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(wfRequest.getServiceName())) {
            params.add(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
        }
        if (!params.isEmpty()) {
            strBuilder.append("Invalid Request. - " + params);
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
                uri.append(configuration.getLmsServiceHost() + configuration.getProfileServiceConfigPath());
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

    private static SBApiResponse createDefaultResponse(String api) {
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
