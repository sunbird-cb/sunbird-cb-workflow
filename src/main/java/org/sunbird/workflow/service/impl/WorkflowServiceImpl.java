package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.sunbird.workflow.models.*;
import org.sunbird.workflow.postgres.entity.WfAuditEntity;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfAuditRepo;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.Workflowservice;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WorkflowServiceImpl implements Workflowservice {

	@Autowired
	private WfStatusRepo wfStatusRepo;

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

	Logger log = LogManager.getLogger(WorkflowServiceImpl.class);

	/**
	 * Change the status of workflow application
	 *
	 * @param rootOrg
	 * @param org
	 * @param wfRequest
	 * @return
	 */

	public Response workflowTransition(String rootOrg, String org, WfRequest wfRequest) {
		HashMap<String, String> changeStatusResponse;
		List<String> wfIds = new ArrayList<>();
		String changedStatus = null;
		if (configuration.getMultipleWfCreationEnable() && !CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
			String wfId = wfRequest.getWfId();
			for (HashMap<String, Object> updatedField : wfRequest.getUpdateFieldValues()) {
				wfRequest.setUpdateFieldValues(new ArrayList<>(Arrays.asList(updatedField)));
				wfRequest.setWfId(wfId);
				changeStatusResponse = changeStatus(rootOrg, org, wfRequest);
				wfIds.add(changeStatusResponse.get(Constants.WF_ID_CONSTANT));
				changedStatus = changeStatusResponse.get(Constants.STATUS);
			}
		} else {
			changeStatusResponse = changeStatus(rootOrg, org, wfRequest);
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

	private HashMap<String, String> changeStatus(String rootOrg, String org, WfRequest wfRequest) {
		String wfId = wfRequest.getWfId();
		String nextState = null;
		HashMap<String, String> data = new HashMap<>();
		try {
			validateWfRequest(wfRequest);
			WfStatusEntity applicationStatus = wfStatusRepo.findByRootOrgAndOrgAndApplicationIdAndWfId(rootOrg, org,
					wfRequest.getApplicationId(), wfRequest.getWfId());
			WorkFlowModel workFlowModel = getWorkFlowConfig(wfRequest.getServiceName());
			WfStatus wfStatus = getWfStatus(wfRequest.getState(), workFlowModel);
			validateUserAndWfStatus(wfRequest, wfStatus, applicationStatus);
			WfAction wfAction = getWfAction(wfRequest.getAction(), wfStatus);

			// actor has proper role to take the workflow action

			nextState = wfAction.getNextState();
			if (ObjectUtils.isEmpty(applicationStatus)) {
				applicationStatus = new WfStatusEntity();
				wfId = UUID.randomUUID().toString();
				applicationStatus.setWfId(wfId);
				applicationStatus.setServiceName(wfRequest.getServiceName());
				applicationStatus.setUserId(wfRequest.getUserId());
				applicationStatus.setApplicationId(wfRequest.getApplicationId());
				applicationStatus.setRootOrg(rootOrg);
				applicationStatus.setOrg(org);
				applicationStatus.setCreatedOn(new Date());
				wfRequest.setWfId(wfId);
			}

			WfStatus wfStatusCheckForNextState = getWfStatus(nextState, workFlowModel);

			applicationStatus.setLastUpdatedOn(new Date());
			applicationStatus.setCurrentStatus(nextState);
			applicationStatus.setActorUUID(wfRequest.getActorUserId());
			applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
			applicationStatus.setInWorkflow(!wfStatusCheckForNextState.getIsLastState());
			applicationStatus.setDeptName(wfRequest.getDeptName());
			wfStatusRepo.save(applicationStatus);
			producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
			producer.push(configuration.getWorkflowApplicationTopic(), wfRequest);

		} catch (IOException e) {
			throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
		}
		data.put(Constants.STATUS, nextState);
		data.put(Constants.WF_ID_CONSTANT, wfId);
		return data;
	}

	/**
	 * @param rootOrg        root Org
	 * @param org            org
	 * @param searchCriteria Search Criteria
	 * @return Response of Application Search
	 */
	public Response applicationsSearch(String rootOrg, String org, SearchCriteria searchCriteria, boolean... isSearchEnabled) {
		Response response = null;
		Response wfApplicationSearchResponse = null;
		switch (searchCriteria.getServiceName()) {
			// Below statement will work as OR condition.
			case Constants.PROFILE_SERVICE_NAME:
			case Constants.USER_PROFILE_FLAG_SERVICE:
			case Constants.POSITION_SERVICE_NAME:
			case Constants.ORGANISATION_SERVICE_NAME:
			case Constants.DOMAIN_SERVICE_NAME:
				wfApplicationSearchResponse = applicationSerachOnApplicationIdGroup(rootOrg, searchCriteria, isSearchEnabled);
				List<Map<String, Object>> userProfiles = userProfileWfService.enrichUserData(
						(Map<String, List<WfStatusEntity>>) wfApplicationSearchResponse.get(Constants.DATA), rootOrg);
				response = new Response();
				response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
				response.put(Constants.DATA, userProfiles);
				response.put(Constants.STATUS, HttpStatus.OK);
				break;
			default:
				response = applicationSerachOnApplicationIdGroup(rootOrg, searchCriteria);
				break;
		}
		return response;
	}

	/**
	 * @param rootOrg Root Org
	 * @param wfId    Wf Id
	 * @return Response of workflow process
	 */
	@Override
	public Response getWorkflowProcess(String rootOrg, String wfId) {
		Response response = new Response();
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByRootOrgAndWfId(rootOrg, wfId);
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, wfStatusEntity);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	/**
	 * Validate application against workflow state
	 *
	 * @param wfRequest
	 * @param wfStatus
	 * @param applicationStatus
	 */
	private void validateUserAndWfStatus(WfRequest wfRequest, WfStatus wfStatus, WfStatusEntity applicationStatus) {

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
	 * @param nextActions
	 * @return String of next actions
	 */
	private String getNextApplicableActions(List<WfAction> nextActions) {
		String applicableAction = null;
		List<HashMap<String, Object>> nextActionArray = new ArrayList<>();
		try {
			if (CollectionUtils.isEmpty(nextActions)) {
				applicableAction = mapper.writeValueAsString(nextActions);
			} else {
				HashMap<String, Object> actionMap = null;
				for (WfAction action : nextActions) {
					actionMap = new HashMap<>();
					actionMap.put(Constants.ACTION_CONSTANT, action.getAction());
					actionMap.put(Constants.ROLES_CONSTANT, action.getRoles());
					nextActionArray.add(actionMap);
				}
				applicableAction = mapper.writeValueAsString(nextActionArray);
			}
		} catch (IOException e) {
			throw new ApplicationException(Constants.JSON_PARSING_ERROR, e);
		}
		return applicableAction;
	}

	/**
	 * Validate roles of actor with action role
	 *
	 * @param actorRoles
	 * @param actionRoles
	 */
	private void validateRoles(List<String> actorRoles, List<String> actionRoles) {
		if ((CollectionUtils.isEmpty(actionRoles))
				|| (CollectionUtils.isEmpty(actorRoles) && CollectionUtils.isEmpty(actionRoles)))
			return;
		if (CollectionUtils.isEmpty(actorRoles)) {
			throw new ApplicationException(Constants.WORKFLOW_ROLE_ERROR);
		}
		boolean roleFound = actorRoles.stream().anyMatch(actionRoles::contains);
		if (!roleFound) {
			throw new BadRequestException(Constants.WORKFLOW_ROLE_CHECK_ERROR);
		}
	}

	/**
	 * Get Workflow Action based on given action
	 *
	 * @param action
	 * @param wfStatus
	 * @return Work flow Action
	 */
	private WfAction getWfAction(String action, WfStatus wfStatus) {
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
	 * Get the workflow State based on given state
	 *
	 * @param state
	 * @param workFlowModel
	 * @return Workflow State
	 */
	private WfStatus getWfStatus(String state, WorkFlowModel workFlowModel) {
		WfStatus wfStatus = null;
		for (WfStatus status : workFlowModel.getWfstates()) {
			if (status.getState().equals(state)) {
				wfStatus = status;
			}
		}
		if (ObjectUtils.isEmpty(wfStatus)) {
			throw new BadRequestException(Constants.WORKFLOW_STATE_CHECK_ERROR);
		}
		return wfStatus;
	}

	/**
	 * Validate the workflow request
	 *
	 * @param wfRequest
	 */
	private void validateWfRequest(WfRequest wfRequest) {

		if (StringUtils.isEmpty(wfRequest.getState())) {
			throw new InvalidDataInputException(Constants.STATE_VALIDATION_ERROR);
		}

		if (StringUtils.isEmpty(wfRequest.getApplicationId())) {
			throw new InvalidDataInputException(Constants.APPLICATION_ID_VALIDATION_ERROR);
		}

		if (StringUtils.isEmpty(wfRequest.getActorUserId())) {
			throw new InvalidDataInputException(Constants.ACTOR_UUID_VALIDATION_ERROR);
		}

		if (StringUtils.isEmpty(wfRequest.getUserId())) {
			throw new InvalidDataInputException(Constants.USER_UUID_VALIDATION_ERROR);
		}

		if (StringUtils.isEmpty(wfRequest.getAction())) {
			throw new InvalidDataInputException(Constants.ACTION_VALIDATION_ERROR);
		}

		if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
			throw new InvalidDataInputException(Constants.FIELD_VALUE_VALIDATION_ERROR);
		}

		if (StringUtils.isEmpty(wfRequest.getServiceName())) {
			throw new InvalidDataInputException(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
		}

	}

	/**
	 * Get the application based on application id
	 *
	 * @param rootOrg
	 * @param org
	 * @param wfId
	 * @param applicationId
	 * @return Wf Application based on Id
	 */
	public Response getWfApplication(String rootOrg, String org, String wfId, String applicationId) {
		WfStatusEntity applicationStatus = wfStatusRepo.findByRootOrgAndOrgAndApplicationIdAndWfId(rootOrg, org,
				applicationId, wfId);
		List<WfStatusEntity> applicationList = applicationStatus == null ? new ArrayList<>()
				: new ArrayList<>(Arrays.asList(applicationStatus));
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, applicationList);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	/**
	 * Get workflow applications based on status
	 *
	 * @param rootOrg
	 * @param org
	 * @param criteria
	 * @return workflow applications
	 */
	public Response wfApplicationSearch(String rootOrg, String org, SearchCriteria criteria) {
		Pageable pageable = getPageReqForApplicationSearch(criteria);
		Page<WfStatusEntity> statePage = wfStatusRepo.findByRootOrgAndOrgAndServiceNameAndCurrentStatus(rootOrg, org,
				criteria.getServiceName(), criteria.getApplicationStatus(), pageable);
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, statePage.getContent());
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	/**
	 * @param rootOrg
	 * @param wfId
	 * @param applicationId
	 * @return
	 */
	public Response getApplicationHistoryOnWfId(String rootOrg, String wfId, String applicationId) {
		Response response = new Response();
		List<WfAuditEntity> wfAuditEntityList = wfAuditRepo
				.findByRootOrgAndApplicationIdAndWfIdOrderByCreatedOnDesc(rootOrg, applicationId, wfId);
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, wfAuditEntityList);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	/**
	 * @param rootOrg
	 * @param org
	 * @param serviceName
	 * @param state
	 * @return
	 */
	public Response getNextActionForState(String rootOrg, String org, String serviceName, String state) {
		Response response = new Response();
		try {
			WorkFlowModel workFlowModel = getWorkFlowConfig(serviceName);
			WfStatus wfStatus = getWfStatus(state, workFlowModel);
			List<HashMap<String, Object>> nextActionArray = new ArrayList<>();
			HashMap<String, Object> actionMap = null;
			if (!CollectionUtils.isEmpty(wfStatus.getActions())) {
				for (WfAction action : wfStatus.getActions()) {
					actionMap = new HashMap<>();
					actionMap.put(Constants.ACTION_CONSTANT, action.getAction());
					actionMap.put(Constants.ROLES_CONSTANT, action.getRoles());
					actionMap.put(Constants.IS_WORKFLOW_TERMINATED, !wfStatus.getIsLastState());
					nextActionArray.add(actionMap);
				}
			}
			response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
			response.put(Constants.DATA, nextActionArray);
			response.put(Constants.STATUS, HttpStatus.OK);
		} catch (Exception e) {
			throw new ApplicationException(Constants.JSON_PARSING_ERROR, e);
		}
		return response;
	}

	public WfStatus getWorkflowStates(String rootOrg, String org, String serviceName, String state) {
		WfStatus wfStatus = null;
		try {
			WorkFlowModel workFlowModel = getWorkFlowConfig(serviceName);
			wfStatus = getWfStatus(state, workFlowModel);
		} catch (Exception e) {
			throw new ApplicationException(Constants.JSON_PARSING_ERROR, e);
		}
		return wfStatus;
	}

	/**
	 * @param rootOrg
	 * @param applicationId
	 * @return
	 */
	public Response getApplicationWfHistory(String rootOrg, String applicationId) {
		Response response = new Response();
		List<WfAuditEntity> wfAuditEntityList = wfAuditRepo.findByRootOrgAndApplicationIdOrderByCreatedOnDesc(rootOrg,
				applicationId);
		HashMap<String, List<WfAuditEntity>> history = new HashMap<>();

		for (WfAuditEntity audit : wfAuditEntityList) {
			if (StringUtils.isEmpty(history.get(audit.getWfId()))) {
				List<WfAuditEntity> wfAuditEntities = new ArrayList<>(Arrays.asList(audit));
				history.put(audit.getWfId(), wfAuditEntities);
			} else {
				history.get(audit.getWfId()).add(audit);
			}
		}
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, history);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	/**
	 * @param userId userId
	 * @return list of roles for user
	 */
	private List<String> getUserRoles(String userId) {
		List<String> roleList;
		StringBuilder builder = new StringBuilder();
		String endPoint = configuration.getUserRoleSearchEndpoint().replace("{user_id}", userId);
		builder.append(configuration.getLexCoreServiceHost()).append(endPoint);
		Map<String, Object> response = (Map<String, Object>) requestServiceImpl.fetchResultUsingGet(builder);
		List<String> defaultRoles = new ArrayList<>();
		List<String> userRoles = new ArrayList<>();
		if (!ObjectUtils.isEmpty(response.get("default_roles"))) {
			defaultRoles = (List<String>) response.get("default_roles");
		}
		if (!ObjectUtils.isEmpty(response.get("user_roles"))) {
			userRoles = (List<String>) response.get("user_roles");
		}
		roleList = Stream.concat(userRoles.stream(), defaultRoles.stream()).distinct().collect(Collectors.toList());
		return roleList;
	}

	public Response applicationSerachOnApplicationIdGrup(String rootOrg, SearchCriteria criteria) {
		Pageable pageable = getPageReqForApplicationSearch(criteria);

		List<String> applicationIds = criteria.getApplicationIds();
		if (CollectionUtils.isEmpty(applicationIds)) {
			applicationIds = wfStatusRepo.getListOfDistinctApplication(rootOrg, criteria.getServiceName(),
					criteria.getApplicationStatus(), pageable);
		}
		List<WfStatusEntity> wfStatusEntities = wfStatusRepo.findByServiceNameAndCurrentStatusAndApplicationIdIn(
				criteria.getServiceName(), criteria.getApplicationStatus(), applicationIds);
		Map<String, List<WfStatusEntity>> infos = wfStatusEntities.stream()
				.collect(Collectors.groupingBy(WfStatusEntity::getApplicationId));
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, infos);
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

	@Override
	public Response updateUserProfileWF(String rootOrg, String org, WfRequest wfRequest) {
		String wfId;
		WfStatusEntity applicationStatus = new WfStatusEntity();
		wfId = UUID.randomUUID().toString();
		applicationStatus.setWfId(wfId);
		applicationStatus.setServiceName(wfRequest.getServiceName());
		applicationStatus.setUserId(wfRequest.getUserId());
		applicationStatus.setApplicationId(wfRequest.getApplicationId());
		applicationStatus.setRootOrg(rootOrg);
		applicationStatus.setOrg(org);
		applicationStatus.setCreatedOn(new Date());
		applicationStatus.setLastUpdatedOn(new Date());
		applicationStatus.setCurrentStatus(Constants.APPROVED_STATE);
		applicationStatus.setActorUUID(wfRequest.getActorUserId());
		applicationStatus
				.setDeptName(getDepartmentDetails(wfRequest.getUpdateFieldValues().stream().findFirst().get()));
		wfRequest.setWfId(wfId);
		wfRequest.setAction(Constants.APPROVE_STATE);
		wfRequest.setState(Constants.APPROVED_STATE);
		try {
			applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
		} catch (JsonProcessingException e) {
			throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
		}
		applicationStatus.setInWorkflow(false);
		wfStatusRepo.save(applicationStatus);
		producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
		producer.push(configuration.getWorkflowApplicationTopic(), wfRequest);
		Response response = new Response();
		HashMap<String, Object> data = new HashMap<>();
		data.put(Constants.STATUS, Constants.APPROVED_STATE);
		data.put(Constants.WF_IDS_CONSTANT, Arrays.asList(wfId));
		response.put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + Constants.APPROVED_STATE);
		response.put(Constants.DATA, data);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

    /*@Override
	public Response getUserWf(String rootOrg, String org, String wid, SearchCriteria criteria) {
		List<WfStatusEntity> wfStatusEntities = wfStatusRepo.findByRootOrgAndOrgAndServiceNameAndCurrentStatusAndUserId(
				rootOrg, org, criteria.getServiceName(), criteria.getApplicationStatus(), wid);
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, wfStatusEntities);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}*/

	public Response applicationSerachOnApplicationIdGroup(String rootOrg, SearchCriteria criteria, boolean... isSearchEnabled) {
		boolean searchEnabled = (isSearchEnabled.length < 1)?false:isSearchEnabled[0];
		Pageable pageable = getPageReqForApplicationSearch(criteria);
		List<String> applicationIds = criteria.getApplicationIds();
		Map<String, List<WfStatusEntity>> infos = null;
		if (CollectionUtils.isEmpty(applicationIds)) {
			applicationIds = wfStatusRepo.getListOfDistinctApplicationUsingDept(rootOrg, criteria.getServiceName(),
					criteria.getApplicationStatus(), criteria.getDeptName(), pageable);
		}
		List<WfStatusEntity> wfStatusEntities = null;
		if (!StringUtils.isEmpty(criteria.getDeptName())) {
			if (searchEnabled==true) {
				wfStatusEntities = wfStatusRepo.findByServiceNameAndCurrentStatusAndDeptName(criteria.getServiceName(), criteria.getApplicationStatus(), criteria.getDeptName());
			} else {
				wfStatusEntities = wfStatusRepo.findByServiceNameAndCurrentStatusAndDeptNameAndApplicationIdIn(
						criteria.getServiceName(), criteria.getApplicationStatus(), criteria.getDeptName(), applicationIds);
			}
		} else {
			wfStatusEntities = wfStatusRepo.findByServiceNameAndCurrentStatusAndApplicationIdIn(
					criteria.getServiceName(), criteria.getApplicationStatus(), applicationIds);
		}
		infos = wfStatusEntities.stream().collect(Collectors.groupingBy(WfStatusEntity::getApplicationId));
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, infos);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	private String getDepartmentDetails(HashMap<String, Object> updatedDeptvalue) {
		HashMap<String, String> toValue = (HashMap<String, String>) updatedDeptvalue.get("toValue");
		return toValue.get("departmentName").isEmpty() ? "" : toValue.get("departmentName");
	}

	@Override
	public Response getUserWf(String rootOrg, String org, String wid, SearchCriteria criteria) {
		List<WfStatusEntity> wfStatusEntities = wfStatusRepo.findByRootOrgAndOrgAndServiceNameAndCurrentStatusAndUserId(
				rootOrg, org, criteria.getServiceName(), criteria.getApplicationStatus(), wid);
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, wfStatusEntities);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	@Override
	public Response getUserWFApplicationFields(String rootOrg, String org, String wid, SearchCriteria criteria) {
		List<String> updatedFieldValues = wfStatusRepo.findWfFieldsForUser(rootOrg, org, criteria.getServiceName(), criteria.getApplicationStatus(), wid);
		TypeReference<List<HashMap<String, Object>>> typeRef = new TypeReference<List<HashMap<String, Object>>>() {
		};
		Set<String> fieldsSet = new HashSet<>();
		for (String fields : updatedFieldValues) {
			if (!StringUtils.isEmpty(fields)) {
				try {
					List<HashMap<String, Object>> values = mapper.readValue(fields, typeRef);
					for (HashMap<String, Object> wffieldReq : values) {
						HashMap<String, Object> toValueMap = (HashMap<String, Object>) wffieldReq.get("toValue");
						fieldsSet.add(toValueMap.entrySet().iterator().next().getKey());
					}
				} catch (IOException e) {
					log.error("Exception occurred while parsing wf fields!");
                    log.error(e.toString());
				}
			}
		}
		Response response = new Response();
		response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
		response.put(Constants.DATA, fieldsSet);
		response.put(Constants.STATUS, HttpStatus.OK);
		return response;
	}

	public WorkFlowModel getWorkFlowConfig(String serviceName) {
		try {
			Map<String, Object> wfConfig = new HashMap<>();
			StringBuilder uri = new StringBuilder();
			switch (serviceName) {
				case Constants.PROFILE_SERVICE_NAME:
					uri.append(configuration.getLmsServiceHost() + configuration.getProfileServiceConfigPath());
					break;
				case Constants.USER_REGISTRATION_SERVICE_NAME:
					uri.append(configuration.getLmsServiceHost() + configuration.getUserRegistrationServiceConfigPath());
					break;
				case Constants.POSITION_SERVICE_NAME:
					uri.append(configuration.getLmsServiceHost() + configuration.getPositionServiceConfigPath());
					break;
				case Constants.ORGANISATION_SERVICE_NAME:
					uri.append(configuration.getLmsServiceHost() + configuration.getOrgServiceConfigPath());
					break;
				case Constants.DOMAIN_SERVICE_NAME:
					uri.append(configuration.getLmsServiceHost() + configuration.getDomainServiceConfigPath());
					break;
				default:
					break;
			}
			wfConfig = (Map<String, Object>) requestServiceImpl.fetchResultUsingGet(uri);
			Map<String, Object> result = (Map<String, Object>) wfConfig.get(Constants.RESULT);
			Map<String, Object> response = (Map<String, Object>) result.get(Constants.RESPONSE);
			Map<String,Object> wfStates = mapper.readValue((String) response.get(Constants.VALUE),Map.class);
			WorkFlowModel workFlowModel = mapper.convertValue(wfStates, new TypeReference<WorkFlowModel>(){});
			return workFlowModel;
		} catch (Exception e) {
			log.error("Exception occurred while getting work flow config details!");
			throw new ApplicationException(Constants.WORKFLOW_PARSING_ERROR_MESSAGE, e);
		}
	}
}
