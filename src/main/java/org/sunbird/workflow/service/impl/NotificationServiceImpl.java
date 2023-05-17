package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.consumer.ApplicationProcessingConsumer;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.models.WfStatus;
import org.sunbird.workflow.models.notification.Config;
import org.sunbird.workflow.models.notification.NotificationRequest;
import org.sunbird.workflow.models.notification.Template;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.Workflowservice;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class NotificationServiceImpl {

	public static final String EMAILTEMPLATE = "emailtemplate";
	Logger logger = LogManager.getLogger(ApplicationProcessingConsumer.class);

	@Autowired
	private WfStatusRepo wfStatusRepo;

	@Autowired
	private Configuration configuration;

	@Autowired
	private RequestServiceImpl requestService;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private Workflowservice workflowservice;

	@Autowired
	private UserProfileWfServiceImpl userProfileWfService;

	private static final String WORK_FLOW_EVENT_NAME = "workflow_service_notification";

	private static final String USER_NAME_CONSTANT = "user";

	private static final String USER_NAME_TAG = "#userName";

	private static final String STATE_NAME_TAG = "#state";

	private static final String FIELD_KEY_TAG = "#fieldKey";

	private static final String TO_VALUE_TAG = "#toValue";

	private static final String TO_VALUE_CONST = "toValue";

	private static final String MAIL_SUBJECT = "Your request is #state";
	private static final String MDO_MAIL_SUBJECT = "Request for approval";

	private static final String MAIL_BODY = "Your request to update #fieldKey to #toValue is #state.";

	/**
	 * Send notification to the user based on state of application
	 *
	 * @param wfRequest workflow request
	 */
	public void sendNotification(WfRequest wfRequest) {
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByApplicationIdAndWfId(wfRequest.getApplicationId(),
				wfRequest.getWfId());
		WfStatus wfStatus = workflowservice.getWorkflowStates(wfStatusEntity.getRootOrg(), wfStatusEntity.getOrg(),
				wfStatusEntity.getServiceName(), wfStatusEntity.getCurrentStatus());
		try {
			logger.info("Notification workflow status entity, {}", mapper.writeValueAsString(wfStatusEntity));
			logger.info("Notification workflow status model, {}", mapper.writeValueAsString(wfStatus));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if (!ObjectUtils.isEmpty(wfStatus.getNotificationEnable()) && wfStatus.getNotificationEnable() && Constants.APPROVED.equalsIgnoreCase(wfStatus.getState())) {
			logger.info("Enter's in the notification block");
            Set<String> usersId = new HashSet<>();
            usersId.add(wfRequest.getActorUserId());
			usersId.add(wfStatusEntity.getApplicationId());
			HashMap<String, Object> usersObj = userProfileWfService.getUsersResult(usersId);
			Map<String, Object> recipientInfo = (Map<String, Object>)usersObj.get(wfStatusEntity.getApplicationId());
			Map<String, Object> senderInfo = (Map<String, Object>)usersObj.get(wfRequest.getActorUserId());
			Map<String, Object> params = new HashMap<>();
			NotificationRequest request = new NotificationRequest();
			request.setDeliveryType("message");
			request.setIds(Arrays.asList((String)recipientInfo.get("email")));
			request.setMode("email");
			Template template = new Template();
			template.setId(EMAILTEMPLATE);
			Optional<HashMap<String, Object>> updatedFieldValue = wfRequest.getUpdateFieldValues().stream().findFirst();
			if (updatedFieldValue.isPresent()) {
				HashMap<String, Object> toValue = (HashMap<String, Object>) updatedFieldValue.get().get(TO_VALUE_CONST);
				params.put("body", MAIL_BODY.replace(STATE_NAME_TAG, wfStatusEntity.getCurrentStatus()).replace(FIELD_KEY_TAG, toValue.entrySet().iterator().next().getKey())
						.replace(TO_VALUE_TAG, (String)toValue.entrySet().iterator().next().getValue()));
			}
			params.put("orgImageUrl", null);
			template.setParams(params);
			Config config = new Config();
			config.setSubject(MAIL_SUBJECT.replace(STATE_NAME_TAG, wfStatusEntity.getCurrentStatus()));
			config.setSender((String)senderInfo.get("email"));
			Map<String, Object> req = new HashMap<>();
			request.setTemplate(template);
			request.setConfig(config);
			Map<String, List<NotificationRequest>> notificationMap = new HashMap<>();
			notificationMap.put("notifications", Arrays.asList(request));
			req.put("request", notificationMap);
			sendNotification(req);
		}
	}

	public void sendEmailNotification(WfRequest wfRequest) {
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByApplicationIdAndWfId(wfRequest.getApplicationId(),
				wfRequest.getWfId());
		WfStatus wfStatus = workflowservice.getWorkflowStates(wfStatusEntity.getRootOrg(), wfStatusEntity.getOrg(),
				wfStatusEntity.getServiceName(), wfStatusEntity.getCurrentStatus());
		try {
			logger.info("Notification workflow status entity, {}", mapper.writeValueAsString(wfStatusEntity));
			logger.info("Notification workflow status model, {}", mapper.writeValueAsString(wfStatus));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if (!ObjectUtils.isEmpty(wfStatus.getNotificationEnable()) && wfStatus.getNotificationEnable()) {
			logger.info("Enters in the email notification block");
			List<HashMap<String, Object>> updatedFieldValues = wfRequest.getUpdateFieldValues();
			Set<String> emailSet = new HashSet<>();
			// Find the email address from the updated field values
			for (Map<String, Object> fieldValue : updatedFieldValues) {
				if (fieldValue.containsKey("email")) {
					String email = (String) fieldValue.get("email");
					emailSet.add(email);
					break;
				}
			}
			if (!emailSet.isEmpty()) {
				HashMap<String, Object> params = new HashMap<>();
				NotificationRequest request = new NotificationRequest();
				request.setDeliveryType("message");
				request.setIds(new ArrayList<>(emailSet));
				request.setMode("email");
				Template template = new Template();
				template.setId(EMAILTEMPLATE);
				Optional<HashMap<String, Object>> updatedFieldValue = wfRequest.getUpdateFieldValues().stream().findFirst();
				if (updatedFieldValue.isPresent()) {
					HashMap<String, Object> toValue = (HashMap<String, Object>) updatedFieldValue.get().get(TO_VALUE_CONST);
					params.put("body", MAIL_BODY.replace(STATE_NAME_TAG, wfStatusEntity.getCurrentStatus()).replace(FIELD_KEY_TAG, toValue.entrySet().iterator().next().getKey())
							.replace(TO_VALUE_TAG, (String) toValue.entrySet().iterator().next().getValue()));
				}
				params.put("orgImageUrl", null);
				template.setParams(params);
				Config config = new Config();
				config.setSubject(MAIL_SUBJECT.replace(STATE_NAME_TAG, wfStatusEntity.getCurrentStatus()));
				config.setSender(configuration.getSenderMail());
				Map<String, Object> req = new HashMap<>();
				request.setTemplate(template);
				request.setConfig(config);
				Map<String, List<NotificationRequest>> notificationMap = new HashMap<>();
				notificationMap.put("notifications", Arrays.asList(request));
				req.put("request", notificationMap);
				sendNotification(req);
			} else {
				logger.warn("Email address not found in the update field values.");
			}
		}
	}

	public void sendNotificationToMdoAdmin(WfRequest wfRequest) {
		WfStatusEntity wfStatusEntity = wfStatusRepo.findByApplicationIdAndWfId(wfRequest.getApplicationId(),
				wfRequest.getWfId());
		WfStatus wfStatus = workflowservice.getWorkflowStates(wfStatusEntity.getRootOrg(), wfStatusEntity.getOrg(),
				wfStatusEntity.getServiceName(), wfStatusEntity.getCurrentStatus());
		try {
			logger.info("Notification workflow status entity, {}", mapper.writeValueAsString(wfStatusEntity));
			logger.info("Notification workflow status model, {}", mapper.writeValueAsString(wfStatus));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if (!ObjectUtils.isEmpty(wfStatus.getNotificationEnable()) && wfStatus.getNotificationEnable()) {
			logger.info("Enter in the notification block");
			List<Map<String, Object>> mdoAdminList = userProfileWfService.getMdoAdminDetails(wfRequest.getRootOrgId());
			Map<String, Object> params = new HashMap<>();
			NotificationRequest request = new NotificationRequest();
			request.setDeliveryType("message");
			List<String> mdoMailList = mdoAdminList.stream().map(mdoAdmin -> (String) mdoAdmin.get("email")).collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(mdoMailList)) {
				request.setIds(mdoMailList);
				request.setMode("email");
				Template template = new Template();
				template.setId(EMAILTEMPLATE);
				Optional<HashMap<String, Object>> updatedFieldValue = wfRequest.getUpdateFieldValues().stream().findFirst();
				HashMap<String, Object> usersObj = userProfileWfService.getUsersResult(Collections.singleton(wfRequest.getUserId()));
				Map<String, Object> recipientInfo = (Map<String, Object>)usersObj.get(wfStatusEntity.getUserId());
				if (updatedFieldValue.isPresent()) {
					params.put("body", constructMdoEmailBody((String) recipientInfo.get(Constants.FIRST_NAME), updatedFieldValue, wfRequest.getApplicationId()));
				}
				params.put("orgImageUrl", null);
				template.setParams(params);
				Config config = new Config();
				config.setSubject(MDO_MAIL_SUBJECT);
				config.setSender(configuration.getSenderMail());
				Map<String, Object> req = new HashMap<>();
				request.setTemplate(template);
				request.setConfig(config);
				Map<String, List<NotificationRequest>> notificationMap = new HashMap<>();
				notificationMap.put("notifications", Arrays.asList(request));
				req.put("request", notificationMap);
				sendNotification(req);
			}
		}
	}

	public String constructMdoEmailBody(String userName, Optional<HashMap<String, Object>> updatedFieldValue, String applicationId) {
		HashMap<String, Object> toValue = (HashMap<String, Object>) updatedFieldValue.get().get(TO_VALUE_CONST);
		List<String> fieldNames = toValue.keySet().stream().collect(Collectors.toList());
		String requestString = IntStream.range(0, fieldNames.size())
				.mapToObj(i -> "&nbsp;&nbsp;" + (i + 1) + ". " + fieldNames.get(i).toUpperCase())
				.collect(Collectors.joining("<br>"));
		requestString = "Dear Admin, <br><br> &nbsp; You have received the below requests for approval from " + userName + "<br>" + requestString;
		String approvalUrl = configuration.getMdoBaseUrl() + applicationId + Constants.TO_APPROVE_CONST;
		String body = requestString + "<br><br> Please Click <a href='" + approvalUrl + "' style='color: blue;'>here</a> to view the requests.";
		return body;
	}

		/**
         * Post to the Notification service
         * @param request
         */
	public void sendNotification(Map<String, Object> request) {
		StringBuilder builder = new StringBuilder();
		builder.append(configuration.getNotifyServiceHost()).append(configuration.getNotifyServicePath());
		try {
			requestService.fetchResultUsingPost(builder, request, Map.class, null);
		} catch (Exception e) {
			logger.error("Exception while posting the data in notification service: ", e);
		}

	}
}
