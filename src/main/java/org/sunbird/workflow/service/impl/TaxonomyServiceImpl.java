package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
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

    @Value("{$taxonomy.term.read.api}")
    private String termReadURI;

    @Value("{$taxonomy.workflow.live}")
    private String live;

    @Value("{$taxonomy.workflow.publish}")
    private String underPublish;

    @Override
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest) && !StringUtils.isEmpty(wfRequest.getWfId())) {
            WfStatusEntityV2 wfStatusEntityV2 = wfStatusRepoV2.findBywfId(wfRequest.getWfId());
            List<HashMap<String, Object>> updateFieldValues = getUpdateFieldValues(wfStatusEntityV2);
            if (Objects.nonNull(updateFieldValues) && !CollectionUtils.isEmpty(updateFieldValues)) {
                for (HashMap<String, Object> updateFieldValue : updateFieldValues) {
                    String identifier = (String) updateFieldValue.get(Constants.IDENTIFIER);
                    String id = (String) updateFieldValue.get(Constants.CODE);
                    String category = (String) updateFieldValue.get(Constants.CATEGORY);
                    Map<String, Object> termResponse = readTermObject(id, category);
                    if (Constants.OK.equalsIgnoreCase((String) termResponse.get(Constants.RESPONSE_CODE))) {
                        Map<String, Object> resultMap = (Map<String, Object>) termResponse.get(Constants.RESULT);
                        Map<String, Object> resultTerm = (Map<String, Object>) resultMap.get(Constants.TERM);
                        if (!((String) resultTerm.get(Constants.APPROVAL_STATUS)).equals(Constants.Live)) {
                            HashMap<String, Object> request = new HashMap<>();
                            HashMap<String, Object> term = new HashMap<>();
                            HashMap<String, Object> requestMap = new HashMap<>();
                            if (wfStatusEntityV2.getCurrentStatus().equals(Constants.SEND_FOR_REVIEW_LEVEL_1)) {
                                requestMap.put(Constants.APPROVAL_STATUS, under_L1_Review);
                            } else if (wfStatusEntityV2.getCurrentStatus().equals(Constants.SEND_FOR_REVIEW_LEVEL_2)) {
                                requestMap.put(Constants.APPROVAL_STATUS, under_L2_Review);
                            } else if (wfStatusEntityV2.getCurrentStatus().equals(Constants.SEND_FOR_PUBLISH)) {
                                requestMap.put(Constants.APPROVAL_STATUS, underPublish);
                            } else if (wfStatusEntityV2.getCurrentStatus().equals(Constants.APPROVE_STATE)) {
                                requestMap.put(Constants.APPROVAL_STATUS, live);
                            }
                            requestMap.put(Constants.IDENTIFIER, identifier);
                            term.put(Constants.TERM, requestMap);
                            request.put(Constants.REQUEST, term);
                            String URI = constructTermUpdateURI(id, category);
                            logger.info("printing URI For Term Update {} ", URI);
                            Map<String, Object> response = requestService.fetchResultUsingPatch(URI, request, null);
                            String responseCode = (String) response.get(Constants.RESPONSE_CODE);
                            if (!responseCode.equals(Constants.OK)) {
                                logger.error("Unable To Update Term Status");
                            }
                        }
                    }
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
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(frameworkId) && !StringUtils.isEmpty(category)) {
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + TERM_UPDATE_URI.replace(Constants.ID, term)).
                    queryParam(Constants.FRAMEWORK, frameworkId).queryParam(Constants.CATEGORY, category).build();
            uri = uriComponents.toString();
        }
        return uri;
    }

    private StringBuilder constructPublishFrameworkURI() {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(frameworkId)) {
            builder = new StringBuilder();
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + PUBLISH_FRAMEWORK_URI.replace(Constants.ID, frameworkId)).build();
            builder.append(uriComponents);
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

    private StringBuilder constructReadTermApiURI(String term, String category) {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(category)) {
            builder = new StringBuilder();
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + termReadURI.replace(Constants.ID, term))
                    .queryParam(Constants.FRAMEWORK, frameworkId).queryParam(Constants.CATEGORY, category)
                    .build();
            builder.append(uriComponents);
        }
        return builder;
    }

    private Map<String, Object> readTermObject(String term, String category) {
        StringBuilder builder = null;
        Map<String, Object> termResponse = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(category)) {
            builder = new StringBuilder();
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + termReadURI.replace(Constants.ID, term))
                    .queryParam(Constants.FRAMEWORK, frameworkId).queryParam(Constants.CATEGORY, category)
                    .build();
            builder.append(uriComponents);
            termResponse = (Map<String, Object>) requestService.fetchResultUsingGet(builder);
        }
        return termResponse;
    }
}

