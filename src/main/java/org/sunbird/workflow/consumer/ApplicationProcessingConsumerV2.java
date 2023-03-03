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
import org.sunbird.workflow.service.impl.ApplicationProcessingServiceImplV2;

import java.io.IOException;
@Service
public class ApplicationProcessingConsumerV2 {

    Logger logger = LogManager.getLogger(ApplicationProcessingConsumerV2.class);
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationProcessingServiceImplV2 applicationProcessingServiceImplV2;

    @KafkaListener(topicPartitions = {@TopicPartition(topic = "${kafka.topics.workflow.request.v1}", partitions = {"0"})})
    public void processMessage(ConsumerRecord<String,String> data){

         WfRequest wfRequest = null;
        try {
            String extractDataFromTopic = data.value();
            wfRequest = mapper.readValue(extractDataFromTopic, WfRequest.class);
        } catch (IOException e) {
            logger.error("Error while deserialization the object value", e);
        }
        applicationProcessingServiceImplV2.processWfApplicationRequest(wfRequest);
    }
}
