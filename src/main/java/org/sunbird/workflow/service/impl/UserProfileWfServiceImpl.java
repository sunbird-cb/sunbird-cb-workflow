package org.sunbird.workflow.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.core.WFLogger;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.Workflowservice;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserProfileWfServiceImpl implements UserProfileWfService {

//	private WFLogger logger = new WFLogger(getClass().getName());
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
		final ObjectMapper mapper = new ObjectMapper();
		StringBuilder builder = new StringBuilder();
		String endPoint = configuration.getUserProfileReadEndPoint().replace(Constants.USER_ID_VALUE,
				wfRequest.getApplicationId());
		builder.append(configuration.getLmsServiceHost()).append(endPoint);
		Object readResponse = requestServiceImpl.fetchResultUsingGet(builder);
		Map<String, Object> readData = mapper.convertValue(readResponse,Map.class);
		String existingDeptName = (String)((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) readData.get("result")).get("response")).get("rootOrg")).get("orgName");
		String updatedDeptName = wfRequest.getDeptName();
		if(!existingDeptName.equals(updatedDeptName)){
			Map<String, Object> migrateRequestWrapper = new HashMap<>();
			Map<String, Object> migrateRequestObject = new HashMap<>();
			migrateRequestWrapper.put(Constants.USER_ID, wfRequest.getApplicationId());
			migrateRequestWrapper.put(Constants.CHANNEL, configuration.getHubRootOrg());
			migrateRequestWrapper.put(Constants.FORCE_MIGRATION, true);
			migrateRequestWrapper.put(Constants.SOFT_DELETE_OLD_GROUP,true );
			migrateRequestWrapper.put(Constants.NOTIFY_MIGRATION,false );
			migrateRequestObject.put(Constants.REQUEST, migrateRequestWrapper);
			builder = new StringBuilder();
			builder.append(configuration.getLmsServiceHost()).append(configuration.getUserProfileMigrateEndPoint());
			HashMap<String, String> headersValue = new HashMap<>();
			headersValue.put("Content-Type", "application/json");
			Map<String, Object> migrateeUserApiResp = (Map<String, Object>) requestServiceImpl
					.fetchResultUsingPatch(builder, migrateRequestObject, Map.class, headersValue);
		}
		Map<String, Object> profileDetails = (Map<String, Object>)((Map<String, Object>)((Map<String, Object>) readData.get("result")).get("response")).get("profileDetails");
		Map<String, Object> updateRequest = updateRequestWithWF(wfRequest.getApplicationId(),wfRequest.getUpdateFieldValues(), profileDetails);
		Map<String, Object> requestObject = new HashMap<>();
		Map<String, Object> requestWrapper = new HashMap<>();
		requestWrapper.put(Constants.USER_ID, wfRequest.getApplicationId());
		requestWrapper.put(Constants.PROFILE_DETAILS, updateRequest);
		requestObject.put(Constants.REQUEST, requestWrapper);
		HashMap<String, String> headersValue = new HashMap<>();
		headersValue.put("Content-Type", "application/json");
		builder = new StringBuilder();
		builder.append(configuration.getLmsServiceHost()).append(configuration.getUserProfileUpdateEndPoint());
		Map<String, Object> updateUserApiResp = (Map<String, Object>) requestServiceImpl
				.fetchResultUsingPost(builder, requestObject, Map.class, headersValue);
	}


	public Map<String, Object> updateRequestWithWF(String uuid, List<HashMap<String, Object>> requests,Map<String, Object> search) {

		final ObjectMapper mapper = new ObjectMapper();
//		Map<String, Object> search = new HashMap<>();
		// merge request and search to add osid(s)
		try {
//			search = profileUtils.readUserProfiles(uuid);
			logger.info("profile orginal :- {}", mapper.convertValue(search, JsonNode.class));
			for (Map<String, Object> request : requests) {

				String osid = request.get(Constants.OSID) == null ? "" : request.get(Constants.OSID).toString();
				Map<String, Object> toChange = new HashMap<>();
				Object sf = search.get(request.get(Constants.FIELD_KEY));

				if (sf instanceof ArrayList) {
					List<Map<String, Object>> searchFields = (ArrayList) search.get(request.get(Constants.FIELD_KEY));
					for (Map<String, Object> obj : searchFields) {
						if (obj.get(Constants.OSID) != null
								&& obj.get(Constants.OSID).toString().equalsIgnoreCase(osid))
							toChange.putAll(obj);
					}
				}
				if (sf instanceof HashMap) {
					Map<String, Object> searchFields = (Map<String, Object>) search
							.get(request.get(Constants.FIELD_KEY));
					toChange.putAll(searchFields);

				}

				Map<String, Object> objectMap = (Map<String, Object>) request.get(Constants.TO_VALUE);
				for (Map.Entry entry : objectMap.entrySet())
					toChange.put((String) entry.getKey(), entry.getValue());

				mergeLeaf(search, toChange, request.get("fieldKey").toString(), osid);
			}
			logger.info("profile merged changes :- {}", mapper.convertValue(search, JsonNode.class));
		} catch (Exception e) {
			logger.error("Merge profile exception::{}", e);
		}
		return search;
	}


	public static void mergeLeaf(Map<String, Object> mapLeft, Map<String, Object> mapRight, String leafKey, String id) {
		// go over all the keys of the right map

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
		HashMap<String, String> headersValue = new HashMap<>();
		headersValue.put("Content-Type", "application/json");
		try {
			StringBuilder builder = new StringBuilder();
			builder.append(configuration.getLmsServiceHost()).append(configuration.getLmsUserSearchEndPoint());
			Map<String, Object> searchProfileApiResp = (Map<String, Object>) requestServiceImpl
					.fetchResultUsingPost(builder, request, Map.class, headersValue);
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
			logger.error(String.valueOf(e));
			throw new ApplicationException("Hub Service ERROR: ", e);
		}
		return userResult;
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
