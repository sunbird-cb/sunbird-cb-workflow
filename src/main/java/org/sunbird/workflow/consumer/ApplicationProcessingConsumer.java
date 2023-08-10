package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.ApplicationProcessingServiceImpl;
import org.sunbird.workflow.service.impl.WorkflowAuditProcessingServiceImpl;

@Service
public class ApplicationProcessingConsumer {

    Logger logger = LogManager.getLogger(ApplicationProcessingConsumer.class);

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationProcessingServiceImpl applicationProcessingServiceImpl;

    @Autowired
    private WorkflowAuditProcessingServiceImpl workflowAuditProcessingService;


    @KafkaListener(groupId = "workflowContentTopic-consumer", topics = "${kafka.topics.workflow.request}")
    public void processMessage(ConsumerRecord<String, String> data) {
        WfRequest wfRequest = null;
        try {
            String message = String.valueOf(data.value());
            wfRequest = mapper.readValue(message, WfRequest.class);
            logger.info("Recevied data in user profile consumer : {}", mapper.writeValueAsString(wfRequest));
        } catch (Exception ex) {
            logger.error("Error while deserialization the object value", ex);
        }
        applicationProcessingServiceImpl.processWfApplicationRequest(wfRequest);
        workflowAuditProcessingService.createAudit(wfRequest);
        applicationProcessingServiceImpl.updateDepartmentToPortalDBs(wfRequest);
    }
}


