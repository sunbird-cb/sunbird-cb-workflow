package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
import org.sunbird.workflow.postgres.repo.WfStatusRepoV2;
import org.sunbird.workflow.service.WfServiceHandler;

import java.util.*;

@Service("taxonomyServiceImpl")
public class TaxonomyServiceImpl implements WfServiceHandler {

    Logger logger = LogManager.getLogger(TaxonomyServiceImpl.class);

    @Autowired
    private WfStatusRepoV2 wfStatusRepoV2;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private ObjectMapper mapper;

    @Value("${lms.system.host}")
    private String host;

    @Value("${taxonomy.framework.id}")
    private String frameworkId;

    @Value("${taxonomy.channel.id}")
    private String channelId;

    @Value("${taxonomy.workflow.draft.status}")
    private String draft;

    @Value("${taxonomy.workflow.L1.status}")
    private String under_L1_Review;

    @Value("${taxonomy.workflow.L2.status}")
    private String under_L2_Review;

    @Value("${taxonomy.workflow.approved.status}")
    private String approved;

    @Value("${taxonomy.term.update.api}")
    private String TERM_UPDATE_URI;

    @Value("${taxonomy.framework.publish.api}")
    private String PUBLISH_FRAMEWORK_URI;

    @Value("${taxonomy.term.read.api}")
    private String termReadURI;

    @Value("${taxonomy.workflow.live}")
    private String live;

    @Value("${taxonomy.workflow.publish}")
    private String underPublish;

    @Override
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest) && !StringUtils.isEmpty(wfRequest.getWfId())) {
            WfStatusEntityV2 wfStatusEntityV2 = wfStatusRepoV2.findBywfId(wfRequest.getWfId());
            List<HashMap<String, Object>> updateFieldValues = getUpdateFieldValues(wfStatusEntityV2);
            if (Objects.nonNull(updateFieldValues) && !CollectionUtils.isEmpty(updateFieldValues)) {
                for (HashMap<String, Object> updateFieldValue : updateFieldValues) {
                    changeStatus(updateFieldValue,wfStatusEntityV2.getCurrentStatus());
                }
                StringBuilder frameworkURI = constructPublishFrameworkURI();
                HashMap<String, String> headers = new HashMap<>();
                headers.put(Constants.XCHANNELID, channelId);
                Map<String, Object> publishApiResponse = (Map<String, Object>) requestService.fetchResultUsingPost(frameworkURI, null, Map.class, headers);
                String responseCode = (String) publishApiResponse.get(Constants.RESPONSE_CODE);
                if (!responseCode.equals(Constants.OK)) {
                    logger.error("Unable To Publish Your Framework");
                }
            }
        }

    }

    private String constructTermUpdateURI(String term, String category) {
        String uri = null;
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(frameworkId) && !StringUtils.isEmpty(category)) {
            builder = new StringBuilder();
            builder = builder.append(host+TERM_UPDATE_URI.replace(Constants.ID, term))
                    .append(Constants.QUESTION_MARK).append(Constants.FRAMEWORK).append(Constants.EQUAL_TO).append(frameworkId)
                    .append(Constants.AND).append(Constants.CATEGORY).append(Constants.EQUAL_TO).append(category);
        }
        return builder.toString();
    }

    private StringBuilder constructPublishFrameworkURI() {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(frameworkId)) {
            builder = new StringBuilder();
            builder.append(host+PUBLISH_FRAMEWORK_URI.replace(Constants.ID, frameworkId));
        }
        return builder;
    }

    private List<HashMap<String, Object>> getUpdateFieldValues(WfStatusEntityV2 statusEntity) {
        List<HashMap<String, Object>> updateFieldValuesList = null;
        if (!ObjectUtils.isEmpty(statusEntity)) {
            if (!StringUtils.isEmpty(statusEntity.getUpdateFieldValues())) {
                try {
                    updateFieldValuesList = mapper.readValue(statusEntity.getUpdateFieldValues(), new TypeReference<List<HashMap<String, Object>>>() {
                    });
                } catch (Exception ex) {
                    logger.error("Exception occurred while parsing wf fields!");
                }
            }
        }
        return updateFieldValuesList;
    }
    private void changeStatus(Map<String,Object> updateFieldValue, String currentStatus) {
        String termIdentifier = (String) updateFieldValue.get(Constants.IDENTIFIER);
        String termCode = (String) updateFieldValue.get(Constants.CODE);
        String category = (String) updateFieldValue.get(Constants.CATEGORY);

        String approvalStatus;
        switch (currentStatus) {
            case Constants.SEND_FOR_REVIEW_LEVEL_1:
                approvalStatus = under_L1_Review;
                break;
            case Constants.SEND_FOR_REVIEW_LEVEL_2:
                approvalStatus = under_L2_Review;
                break;
            case Constants.SEND_FOR_PUBLISH:
                approvalStatus = underPublish;
                break;
            case Constants.APPROVED:
                approvalStatus = live;
                break;
            default:
                logger.error("Invalid current status {}", currentStatus);
                return;
        }

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> term = new HashMap<>();
        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put(Constants.APPROVAL_STATUS, approvalStatus);
        requestMap.put(Constants.IDENTIFIER, termIdentifier);
        List<HashMap<String, Object>> associationsList = (List<HashMap<String, Object>>) updateFieldValue.get(Constants.ASSOCIATIONS);;
        List<Map<String, Object>> association=null;
        if (!CollectionUtils.isEmpty(associationsList)) {
            for (Map associations : associationsList)
            {
                Map<String,Object> map = (Map) associations.get(Constants.ASSOCIATION_PROPERTIES);
                String associationStatus = (String) map.get(Constants.APPROVAL_STATUS);
                Map<String, Object> associationMap = new HashMap<>();
                if (!associationStatus.equals(Constants.Live)) {
                    if (CollectionUtils.isEmpty(association)) {
                        association = new ArrayList<>();
                    }
                    associationMap.put(Constants.APPROVAL_STATUS, approvalStatus);
                    associationMap.put(Constants.IDENTIFIER, (String) associations.get(Constants.IDENTIFIER));
                } else {
                    associationMap.put(Constants.APPROVAL_STATUS, Constants.Live);
                    associationMap.put(Constants.IDENTIFIER, (String) associations.get(Constants.IDENTIFIER));
                }
                association.add(associationMap);
                requestMap.put(Constants.ASSOCIATIONS, association);
            }
        }
        term.put(Constants.TERM, requestMap);
        request.put(Constants.REQUEST,term);

        String URI = constructTermUpdateURI(termCode, category);
        logger.info("Printing URI for Term Update {}", URI);

        Map<String, Object> response = requestService.fetchResultUsingPatch(URI, request, null);
        String responseCode = (String) response.get(Constants.RESPONSE_CODE);
        if (!responseCode.equals(Constants.OK)) {
            logger.error("Unable to update term status");
        }
    }
}

