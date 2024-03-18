package org.sunbird.workflow.consumer;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.service.UserBulkUploadService;

@Service
public class UserBulkUpdateConsumer {

    @Autowired
    UserBulkUploadService userBulkUploadService;

    Logger logger = LogManager.getLogger(UserBulkUpdateConsumer.class);

    @KafkaListener(groupId = "workflowUserBulkUpdateTopic-consumer", topics = "${kafka.topics.user.bulk.update}")
    public void procesBulkUploadForUserUpdate(ConsumerRecord<String, String> data) {
        logger.info(
                "UserBulkUploadConsumer::processMessage: Received event to initiate User Bulk Upload Process...");
        logger.info("Received message:: {}", data.value());
        try {
            if (StringUtils.isNoneBlank(data.value())) {
                CompletableFuture.runAsync(() -> userBulkUploadService.initiateUserBulkUploadProcess(data.value()));
            } else {
                logger.error("Error in User Bulk Upload Consumer: Invalid Kafka Msg");
            }
        } catch (Exception e) {
            logger.error(String.format("Error in User Bulk Upload Consumer: Error Msg :%s", e.getMessage()), e);
        }
    }
}
