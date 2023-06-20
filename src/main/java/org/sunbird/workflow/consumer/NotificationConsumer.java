package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.NotificationServiceImpl;

@Service
public class NotificationConsumer {
	Logger logger = LogManager.getLogger(ApplicationProcessingConsumer.class);

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private NotificationServiceImpl notificationService;

	@KafkaListener(id = "id2", groupId = "workflowNotificationTopic-consumer", topicPartitions = {
			@TopicPartition(topic = "${kafka.topics.workflow.notification}", partitions = { "0", "1", "2", "3" }) })
	public void processMessage(ConsumerRecord<String, String> data) {
		WfRequest wfRequest = null;
		try {
			String message = String.valueOf(data.value());
			wfRequest = mapper.readValue(message, WfRequest.class);
			logger.info("Recevied data in notification consumer : {}", mapper.writeValueAsString(wfRequest));
		} catch (Exception ex) {
			logger.error("Error while deserialization the object value", ex);
		}
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
			case Constants.Blended_Program_SERVICE_NAME:
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
	}
}
