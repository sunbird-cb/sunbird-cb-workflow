package org.sunbird.workflow.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.NotificationServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NotificationConsumer {
	Logger logger = LogManager.getLogger(NotificationConsumer.class);

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private NotificationServiceImpl notificationService;

	@KafkaListener(groupId = "workflowNotificationTopic-consumer", topics = "${kafka.topics.workflow.notification}")
	public void processMessage(ConsumerRecord<String, String> data) {
		WfRequest wfRequest = null;
		try {
			String message = String.valueOf(data.value());
			wfRequest = mapper.readValue(message, WfRequest.class);
			logger.info("Recevied data in notification consumer : {}", mapper.writeValueAsString(wfRequest));
			switch (wfRequest.getServiceName()) {
				case Constants.PROFILE_SERVICE_NAME:
					notificationService.sendNotification(wfRequest);
					notificationService.sendNotificationToMdoAdmin(wfRequest);
					break;
				case Constants.POSITION_SERVICE_NAME:
				case Constants.DOMAIN_SERVICE_NAME:
				case Constants.ORGANISATION_SERVICE_NAME:
					notificationService.sendEmailNotification(wfRequest);
					break;
				case Constants.BLENDED_PROGRAM_SERVICE_NAME:
					notificationService.sendNotification(wfRequest);
					notificationService.sendNotificationToMdoAdminAndPC(wfRequest);
					break;
				case Constants.USER_REGISTRATION_SERVICE_NAME:
					// nothing to do
					break;
				default:
					logger.error("Unsupported ServiceName in WFRequest.");
					break;
			}
		} catch (Exception ex) {
			logger.error("Error while deserialization the object value", ex);
		}
	}
}
