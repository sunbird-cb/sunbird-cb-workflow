package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.exception.BadRequestException;
import org.sunbird.workflow.exception.InvalidDataInputException;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.V2.WfStatusV2;
import org.sunbird.workflow.models.V2.WorkFlowModelV2;
import org.sunbird.workflow.models.WfAction;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
import org.sunbird.workflow.postgres.repo.WfAuditRepo;
import org.sunbird.workflow.postgres.repo.WfStatusRepoV2;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.WorkflowServiceV2;
import org.sunbird.workflow.util.RequestInterceptor;

import java.io.IOException;
import java.util.*;

@Service
public class WorkflowServiceImplV2 implements WorkflowServiceV2 {

    @Autowired
    private WfStatusRepoV2 wfStatusRepoV2;

    @Autowired
    private WfAuditRepo wfAuditRepo;

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
    public Response workflowTransition(String userToken, WfRequest wfRequest) {
        HashMap<String, String> changeStatusResponse;
        List<String> wfIds = new ArrayList<>();
        String changedStatus = null;
        String userId = validateAuthTokenAndFetchUserId(userToken);
        if (configuration.getMultipleWfCreationEnable() && !CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
            String wfId = wfRequest.getWfId();
            for (HashMap<String, Object> updatedField : wfRequest.getUpdateFieldValues()) {
                wfRequest.setUpdateFieldValues(new ArrayList<>(Arrays.asList(updatedField)));
                wfRequest.setWfId(wfId);
                changeStatusResponse = changeStatus(wfRequest,userId);
                wfIds.add(changeStatusResponse.get(Constants.WF_ID_CONSTANT));
                changedStatus = changeStatusResponse.get(Constants.STATUS);
            }
        } else {
            changeStatusResponse = changeStatus(wfRequest, userId);
            wfIds.add(changeStatusResponse.get(Constants.WF_ID_CONSTANT));
            changedStatus = changeStatusResponse.get(Constants.STATUS);
        }
        Response response = new Response();
        HashMap<String, Object> data = new HashMap<>();
        data.put(Constants.STATUS, changedStatus);
        data.put(Constants.WF_IDS_CONSTANT, wfIds);
        response.put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + changedStatus);
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     * Get the application based on wfId
     * @param userToken
     * @param wfId
     * @return Wf Application based on wfId
     */
    @Override
    public Response getWfApplication(String userToken, String wfId) {
        String userId = validateAuthTokenAndFetchUserId(userToken);
        WfStatusEntityV2 applicationStatus = wfStatusRepoV2.findByUserIdAndWfId(userId,wfId);
        List<WfStatusEntityV2> applicationList = applicationStatus == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(applicationStatus));
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, applicationList);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     * Get workflow applications based on status
     * @param userToken
     * @param criteria
     * @return workflow applications
     */
    @Override
    public Response wfApplicationSearch(String userToken, SearchCriteria criteria) {
        //user token need to implement
        Pageable pageable = getPageReqForApplicationSearch(criteria);
        Page<WfStatusEntityV2> statePage = wfStatusRepoV2.findByServiceNameAndCurrentStatus
                (criteria.getServiceName(), criteria.getApplicationStatus(), pageable);
        Response response = new Response();
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, statePage.getContent());
        response.put(Constants.STATUS, HttpStatus.OK);
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
        pageable = PageRequest.of(offset, limit + offset);
        return pageable;
    }

    private HashMap<String, String> changeStatus(WfRequest wfRequest, String userId) {
        String wfId = wfRequest.getWfId();
        String nextState = null;
        HashMap<String, String> data = new HashMap<>();
        try {
            validateRequest(wfRequest);
            //instead of getting userId from the request object collect the userId from the auth token
            WfStatusEntityV2 applicationStatus = wfStatusRepoV2.findByUserIdAndWfId(userId, wfRequest.getWfId());
            WorkFlowModelV2 workFlowModel = getWorkFlowConfig(wfRequest.getServiceName());
            WfStatusV2 wfStatus = getWfStatus(wfRequest.getState(), workFlowModel);
            validateUserAndWfStatus(wfRequest, wfStatus, applicationStatus);
            WfAction wfAction = getWfAction(wfRequest.getAction(), wfStatus);

            // actor has proper role to take the workflow action

            nextState = wfAction.getNextState();
            if (ObjectUtils.isEmpty(applicationStatus)) {
                applicationStatus = new WfStatusEntityV2();
                wfId = UUID.randomUUID().toString();
                applicationStatus.setWfId(wfId);
                applicationStatus.setServiceName(wfRequest.getServiceName());
                applicationStatus.setUserId(wfRequest.getUserId());
                applicationStatus.setCreatedOn(new Date());
                wfRequest.setWfId(wfId);
            }

            WfStatusV2 wfStatusCheckForNextState = getWfStatus(nextState, workFlowModel);

            applicationStatus.setLastUpdatedOn(new Date());
            applicationStatus.setCurrentStatus(nextState);
            applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
            applicationStatus.setInWorkflow(!wfStatusCheckForNextState.getIsLastState());
            wfStatusRepoV2.save(applicationStatus);
            producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
            producer.push(configuration.getWorkflowApplicationTopic(), wfRequest);

        } catch (IOException e) {
            throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
        }
        data.put(Constants.WF_ID_CONSTANT, wfId);
        data.put(Constants.STATUS, nextState);
        return data;
    }

    /**
     * Validate the workflow request
     *
     * @param wfRequest
     */
    private void validateRequest(WfRequest wfRequest) {
        if (StringUtils.isEmpty(wfRequest.getWfId())) {
            if (!wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                throw new InvalidDataInputException(Constants.STATUS_VALIDATION_ERROR_FOR_INITIATE);
            }
        } else {
            if (wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                throw new InvalidDataInputException(Constants.STATUS_VALIDATION_ERROR_FOR_NOT_INITIATE);
            }
        }
        if (StringUtils.isEmpty(wfRequest.getUserId())) {
            throw new InvalidDataInputException(Constants.USER_UUID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getAction())) {
            throw new InvalidDataInputException(Constants.ACTION_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(wfRequest.getServiceName())) {
            throw new InvalidDataInputException(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
        }
        if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
            if (wfRequest.getState().equalsIgnoreCase(Constants.INITIATE)) {
                throw new InvalidDataInputException(Constants.FIELD_VALUE_VALIDATION_ERROR);
            }
        }
    }

    /**
     * Validate application against workflow state
     *
     * @param wfRequest
     * @param wfStatus
     * @param applicationStatus
     */
    private void validateUserAndWfStatus(WfRequest wfRequest, WfStatusV2 wfStatus, WfStatusEntityV2 applicationStatus) {

        if (StringUtils.isEmpty(wfRequest.getWfId()) && !wfStatus.getStartState()) {
            throw new ApplicationException(Constants.WORKFLOW_ID_ERROR_MESSAGE);
        }

        if ((!ObjectUtils.isEmpty(applicationStatus))
                && (!wfRequest.getState().equalsIgnoreCase(applicationStatus.getCurrentStatus()))) {
            throw new BadRequestException("Application is in " + applicationStatus.getCurrentStatus()
                    + " State but trying to be move in " + wfRequest.getState() + " state!");
        }
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
            throw new BadRequestException(Constants.WORKFLOW_ACTION_ERROR);
        }
        for (WfAction filterAction : wfStatus.getActions()) {
            if (action.equals(filterAction.getAction())) {
                wfAction = filterAction;
            }
        }
        if (ObjectUtils.isEmpty(wfAction)) {
            throw new BadRequestException(Constants.WORKFLOW_ACTION_ERROR);
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
            WorkFlowModelV2 workFlowModel = mapper.convertValue(wfStates, new TypeReference<WorkFlowModelV2>() {
            });
            return workFlowModel;
        } catch (Exception e) {
            log.error("Exception occurred while getting work flow config details!");
            throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
        }
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
        if (ObjectUtils.isEmpty(wfStatus)) {
            throw new BadRequestException(Constants.WORKFLOW_STATE_CHECK_ERROR);
        }
        return wfStatus;
    }

    private String validateAuthTokenAndFetchUserId(String authUserToken) {
        return requestInterceptor.fetchUserIdFromAccessToken(authUserToken);
    }
}
