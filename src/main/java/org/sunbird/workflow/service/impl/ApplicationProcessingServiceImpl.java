package org.sunbird.workflow.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.service.UserRegistrationWfService;

@Service
public class ApplicationProcessingServiceImpl {

	@Autowired
	private UserProfileWfService userProfileWfService;

	@Autowired
	private Configuration configuration;
	@Autowired
	private RequestServiceImpl requestService;
	@Autowired
	private WfStatusRepo wfStatusRepo;

	@Autowired
	private UserRegistrationWfService userRegService;

	Logger logger = LogManager.getLogger(ApplicationProcessingServiceImpl.class);

	public void processWfApplicationRequest(WfRequest wfRequest) {
		switch (wfRequest.getServiceName()) {
		// Or condition in case statement
		case Constants.PROFILE_SERVICE_NAME:
		case Constants.USER_PROFILE_FLAG_SERVICE:
			userProfileWfService.updateUserProfile(wfRequest);
			break;
		case Constants.USER_REGISTRATION_SERVICE_NAME:
			userRegService.processMessage(wfRequest);
			break;
	//	case Constants.Blended_Program_SERVICE_NAME:

		default:
			break;
		}
	}

	public void updateDepartmentToPortalDBs(WfRequest wfRequest) {
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByApplicationIdAndWfId(wfRequest.getApplicationId(),
				wfRequest.getWfId());
		if (Constants.PROFILE_SERVICE_NAME.equals(wfRequest.getServiceName())
				&& Constants.APPROVED_STATE.equals(wfStatusEntity.getCurrentStatus())) {
			for (HashMap<String, Object> updatedValues : wfRequest.getUpdateFieldValues()) {
				if (ObjectUtils.isEmpty(updatedValues.get("fieldKey")))
					continue;
				if ("professionalDetails".equals(updatedValues.get("fieldKey"))) {
					HashMap<String, Object> fromValue = (HashMap<String, Object>) updatedValues.get("fromValue");
					HashMap<String, Object> toValue = (HashMap<String, Object>) updatedValues.get("toValue");
					String key = toValue.entrySet().iterator().next().getKey();
					if (!StringUtils.isEmpty(key) && key.equals("name")) {
						String newDeptName = (String) toValue.entrySet().iterator().next().getValue();
						String oldDeptName = (String) fromValue.entrySet().iterator().next().getValue();
						HashMap<String, Object> departMentInfo = new HashMap<>();
						departMentInfo.put("userId", wfRequest.getApplicationId());
						departMentInfo.put("oldDeptName", oldDeptName);
						departMentInfo.put("newDeptName", newDeptName);
						departMentInfo.put("isActive", true);
						departMentInfo.put("isBlocked", false);
						updateUserDepartment(wfStatusEntity.getRootOrg(), wfStatusEntity.getOrg(),
								wfRequest.getApplicationId(), departMentInfo);
					}
				}
			}
		}
	}

	public void updateUserDepartment(String rootOrg, String org, String wid, HashMap<String, Object> departMentInfo) {
		StringBuilder builder = new StringBuilder();
		builder.append(configuration.getLexCoreServiceHost()).append(configuration.getDepartmentUpdatePath());
		try {
			HashMap<String, String> headers = new HashMap<>();
			headers.put(Constants.ROOT_ORG_CONSTANT, rootOrg);
			headers.put("org", org);
			headers.put("wid", wid);
			requestService.fetchResultUsingPost(builder, departMentInfo, Map.class, headers);
		} catch (Exception e) {
			logger.error("Exception while posting the data in portal service: ", e);
		}
	}
}
