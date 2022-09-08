package org.sunbird.workflow.service.impl;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.Workflowservice;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserProfileWfServiceImpl implements UserProfileWfService {

	private Logger logger = LoggerFactory.getLogger(UserProfileWfServiceImpl.class);

	@Autowired
	private Workflowservice workflowservice;

	@Autowired
	private RequestServiceImpl requestServiceImpl;

	@Autowired
	private Configuration configuration;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private WfStatusRepo wfStatusRepo;

	@Autowired
	WorkflowServiceImpl workflowService;

	@Autowired
	private WorkflowAuditProcessingServiceImpl workflowAuditProcessingService;

	/**
	 * Update user profile based on wf request
	 *
	 * @param wfRequest Workflow request
	 */
	public void updateUserProfile(WfRequest wfRequest) {
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByApplicationIdAndWfId(wfRequest.getApplicationId(),
				wfRequest.getWfId());
		if (Constants.PROFILE_SERVICE_NAME.equals(wfRequest.getServiceName())
				&& Constants.APPROVED_STATE.equals(wfStatusEntity.getCurrentStatus())) {
			updateProfile(wfRequest);
		}
		if (Constants.USER_PROFILE_FLAG_SERVICE.equals(wfRequest.getServiceName())
				&& Constants.PROCESSED_STATE.equals(wfStatusEntity.getCurrentStatus())) {
			updateProfile(wfRequest);
		}
	}

	private void updateProfile(WfRequest wfRequest) {
		try {
			Map<String, Object> readData = (Map<String, Object>) userProfileRead(wfRequest.getApplicationId());
			if (null != readData && !Constants.OK.equals(readData.get(Constants.RESPONSE_CODE))) {
				logger.error("user not found" + ((Map<String, Object>) readData.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
				failedCase(wfRequest);
				return;
			}
			Map<String, Object> existingUserResults = (Map<String, Object>) readData.get(Constants.RESULT);
			Map<String, Object> existingUserResponse = (Map<String, Object>) existingUserResults.get(Constants.RESPONSE);
			Map<String, Object> existingUserRootOrgMap = (Map<String, Object>) existingUserResponse.get(Constants.ROOT_ORG_CONSTANT);
			String existingDeptName = (String) existingUserRootOrgMap.get(Constants.ORG_NAME);
			String updatedDeptName = wfRequest.getDeptName();
			if (!existingDeptName.equals(updatedDeptName)) {
				Map<String, Object> response = (Map<String, Object>) migrateUser(wfRequest);
				if (null != response && !Constants.OK.equals(response.get(Constants.RESPONSE_CODE))) {
					logger.error("Migrate user failed" + ((Map<String, Object>) response.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
					failedCase(wfRequest);
					return;
				}

			}
			Map<String, Object> profileDetails = (Map<String, Object>) existingUserResponse.get(Constants.PROFILE_DETAILS);
			Map<String, Object> updateRequest = updateRequestWithWF(wfRequest.getApplicationId(), wfRequest.getUpdateFieldValues(), profileDetails);
			if (null == updateRequest) {
				logger.error("user profile datatype error");
				failedCase(wfRequest);
				return;
			}
			StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
			builder.append(configuration.getUserProfileUpdateEndPoint());
			Map<String, Object> updateUserApiResp = (Map<String, Object>) requestServiceImpl
					.fetchResultUsingPost(builder, getUpdateRequest(wfRequest, updateRequest), Map.class, getHeaders());
			if (null != updateUserApiResp && !Constants.OK.equals(updateUserApiResp.get(Constants.RESPONSE_CODE))) {
				logger.error("user update failed" + ((Map<String, Object>) updateUserApiResp.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
				failedCase(wfRequest);
			}
		} catch (Exception e) {
			logger.error("Exception occurred : ", e);
		}
	}

	public Map<String, Object> updateRequestWithWF(String uuid, List<HashMap<String, Object>> wfRequestParamList, Map<String, Object> existingProfileDetail) {
		try {
			// merge wfRequestParamList and existingProfileDetail to add osid(s)
			for (Map<String, Object> wfRequestParamObj : wfRequestParamList) {
				String osid = wfRequestParamObj.get(Constants.OSID) == null ? "" : wfRequestParamObj.get(Constants.OSID).toString();
				Map<String, Object> updatedProfileElement = new HashMap<>();
				Object updatedProfileElementObj = existingProfileDetail.get(wfRequestParamObj.get(Constants.FIELD_KEY));
				if (updatedProfileElementObj instanceof ArrayList) {
					List<Map<String, Object>> existingProfileElementList = mapper.convertValue(updatedProfileElementObj, ArrayList.class);
					for (Map<String, Object> existingProfileElement : existingProfileElementList) {
						if (existingProfileElement.get(Constants.OSID) != null
								&& existingProfileElement.get(Constants.OSID).toString().equalsIgnoreCase(osid))
							updatedProfileElement.putAll(existingProfileElement);
					}
				} else if (updatedProfileElementObj instanceof HashMap) {
					Map<String, Object> existingProfileElementList = mapper.convertValue(updatedProfileElementObj, Map.class);
					updatedProfileElement.putAll(existingProfileElementList);
				} else {
					logger.error("profile element to be updated is neither arraylist nor hashmap");
					return null;
				}
				Map<String, Object> objectMap = (Map<String, Object>) wfRequestParamObj.get(Constants.TO_VALUE);
				for (Map.Entry entry : objectMap.entrySet())
					updatedProfileElement.put((String) entry.getKey(), entry.getValue());
				mergeLeaf(existingProfileDetail, updatedProfileElement, wfRequestParamObj.get("fieldKey").toString(), osid);
			}
		} catch (Exception e) {
			logger.error("Merge profile exception::{}", e);
		}
		return existingProfileDetail;
	}

	public static void mergeLeaf(Map<String, Object> mapLeft, Map<String, Object> mapRight, String leafKey, String id) {
		if (mapLeft.containsKey(leafKey)) {
			for (String key : mapLeft.keySet()) {
				if (mapLeft.get(key) instanceof ArrayList) {
					Set<String> childRequest = mapRight.keySet();
					for (String keys : childRequest) {
						List<Map<String, Object>> childExisting = (List<Map<String, Object>>) mapLeft.get(key);
						Map<String, Object> childExistingIndex = (Map<String, Object>) childExisting.get(0);
						childExistingIndex.put(keys, mapRight.get(keys));
					}
				}
				if (key.equalsIgnoreCase(leafKey) && (mapLeft.get(key) instanceof HashMap)) {
					mapLeft.put(key, mapRight);
				}
			}
		} else {
			if (leafKey.equals(Constants.PROFESSIONAL_DETAILS)) {
				List<Map<String, Object>> professionalData = new ArrayList<>();
				Map<String, Object> professionalDataChild = new HashMap<>();
				Set<String> childRequest = mapRight.keySet();
				for (String keys : childRequest) {
					professionalDataChild.put(keys, mapRight.get(keys));
				}
				professionalData.add(professionalDataChild);
				mapLeft.put(Constants.PROFESSIONAL_DETAILS, professionalData);
			}
		}
	}

	/**
	 *
	 * @param wfInfos
	 * @param rootOrg
	 * @return
	 */
	public List<Map<String, Object>> enrichUserData(Map<String, List<WfStatusEntity>> wfInfos, String rootOrg) {
		List<Map<String, Object>> wfDetails = new ArrayList<>();
		HashMap<String, Object> responseMap;
		HashMap<String, Object> userResult = null;
		Set<String> userIds = wfInfos.keySet();
		if (!CollectionUtils.isEmpty(userIds)) {
			List<String> sources = new ArrayList<>(Constants.USER_DEFAULT_FIELDS);
			if (!sources.contains(Constants.UUID)) {
				sources.add(Constants.UUID);
			}
			userResult = getUsersResult(userIds);
		}
		for (Map.Entry<String, List<WfStatusEntity>> wfStatusEntity : wfInfos.entrySet()) {
			responseMap = new HashMap<>();
			responseMap.put("wfInfo", wfStatusEntity.getValue());
			responseMap.put("userInfo", userResult.get(wfStatusEntity.getKey()));
			wfDetails.add(responseMap);
		}
		return wfDetails;
	}

	public HashMap<String, Object> getUsersResult(Set<String> userIds) {
		HashMap<String, Object> userResult = new HashMap<>();
		Map<String, Object> request = getSearchObject(userIds);
		try {
			StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
			builder.append(configuration.getLmsUserSearchEndPoint());
			Map<String, Object> searchProfileApiResp = (Map<String, Object>) requestServiceImpl
					.fetchResultUsingPost(builder, request, Map.class, getHeaders());
			if (searchProfileApiResp != null
					&& "OK".equalsIgnoreCase((String) searchProfileApiResp.get(Constants.RESPONSE_CODE))) {
				Map<String, Object> map = (Map<String, Object>) searchProfileApiResp.get(Constants.RESULT);
				Map<String, Object> response = (Map<String, Object>) map.get(Constants.RESPONSE);
				List<Map<String, Object>> contents = (List<Map<String, Object>>) response.get(Constants.CONTENT);
				if (!CollectionUtils.isEmpty(contents)) {
					for (Map<String, Object> content : contents) {
						HashMap<String, Object> profileDetails = (HashMap<String, Object>) content
								.get(Constants.PROFILE_DETAILS);
						if (!CollectionUtils.isEmpty(profileDetails)) {
							HashMap<String, Object> personalDetails = (HashMap<String, Object>) profileDetails
									.get(Constants.PERSONAL_DETAILS);
							if (!CollectionUtils.isEmpty(personalDetails)) {
								Map<String, Object> record = new HashMap<>();
								record.put(Constants.UUID, content.get(Constants.USER_ID));
								record.put(Constants.FIRST_NAME, personalDetails.get(Constants.FIRSTNAME));
								record.put(Constants.LAST_NAME, personalDetails.get(Constants.LASTNAME));
								record.put(Constants.EMAIL, personalDetails.get(Constants.PRIMARY_EMAIL));
								userResult.put(record.get(Constants.UUID).toString(), record);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching user setails : ",e);
			throw new ApplicationException("Hub Service ERROR: ", e);
		}
		return userResult;
	}

	private Object userProfileRead(String userId) {
		StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
		String endPoint = configuration.getUserProfileReadEndPoint().replace(Constants.USER_ID_VALUE, userId);
		builder.append(endPoint);
		Object response = requestServiceImpl.fetchResultUsingGet(builder);
		if (null == response){
			return null;
		}
		Map<String, Object> readResponse = mapper.convertValue(response, Map.class);
		return readResponse;
	}

	private Object migrateUser(WfRequest wfRequest) {

		Map<String, Object> migrateRequestWrapper = new HashMap<>();
		Map<String, Object> migrateRequestObject = new HashMap<>();
		migrateRequestWrapper.put(Constants.USER_ID, wfRequest.getApplicationId());
		migrateRequestWrapper.put(Constants.CHANNEL, wfRequest.getDeptName());
		migrateRequestWrapper.put(Constants.FORCE_MIGRATION, true);
		migrateRequestWrapper.put(Constants.SOFT_DELETE_OLD_GROUP, true);
		migrateRequestWrapper.put(Constants.NOTIFY_MIGRATION, false);
		migrateRequestObject.put(Constants.REQUEST, migrateRequestWrapper);
		StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
		builder.append(configuration.getUserProfileMigrateEndPoint());
		Map<String, Object> migrateUserApiResp = (Map<String, Object>) requestServiceImpl
				.fetchResultUsingPatch(builder, migrateRequestObject, Map.class, getHeaders());
		return migrateUserApiResp;
	}

	private void failedCase(WfRequest wfRequest) {
		wfRequest.setState(Constants.FAILED);
		wfRequest.setAction(Constants.FAILED);
		WfStatusEntity applicationStatus = new WfStatusEntity();
		String wfId = wfRequest.getWfId();
		applicationStatus.setWfId(wfId);
		applicationStatus.setServiceName(wfRequest.getServiceName());
		applicationStatus.setUserId(wfRequest.getUserId());
		applicationStatus.setApplicationId(wfRequest.getApplicationId());
		applicationStatus.setRootOrg(Constants.ROOT_ORG);
		applicationStatus.setOrg(Constants.ORG);
		applicationStatus.setCreatedOn(new Date());
		applicationStatus.setLastUpdatedOn(new Date());
		applicationStatus.setCurrentStatus(Constants.REJECTED);
		applicationStatus.setActorUUID(wfRequest.getActorUserId());
		try {
			applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
		} catch (JsonProcessingException e) {
			logger.error("Exception occurred : ", e);
		}
		applicationStatus.setInWorkflow(false);
		applicationStatus.setDeptName(wfRequest.getDeptName());
		wfStatusRepo.save(applicationStatus);
	}

	private Map<String, Object> getUpdateRequest(WfRequest wfRequest, Map<String, Object> updateRequest) {
		Map<String, Object> requestObject = new HashMap<>();
		Map<String, Object> requestWrapper = new HashMap<>();
		requestWrapper.put(Constants.USER_ID, wfRequest.getApplicationId());
		requestWrapper.put(Constants.PROFILE_DETAILS, updateRequest);
		requestObject.put(Constants.REQUEST, requestWrapper);
		return requestObject;
	}

	private HashMap<String, String> getHeaders() {
		HashMap<String, String> headersValue = new HashMap<>();
		headersValue.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
		return headersValue;
	}

		private Map<String, Object> getSearchObject(Set<String> userIds) {
		Map<String, Object> requestObject = new HashMap<>();
		Map<String, Object> request = new HashMap<>();
		Map<String, Object> filters = new HashMap<>();
		filters.put("userId", userIds);
		request.put("limit", userIds.size());
		request.put("offset", 0);
		request.put("filters", filters);
		requestObject.put("request", request);
		return requestObject;
	}
}
