package org.sunbird.workflow.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.consumer.ApplicationProcessingConsumer;
import org.sunbird.workflow.core.WFLogger;
import org.sunbird.workflow.models.Request;
import org.sunbird.workflow.models.RequestTerm;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.WfServiceHandler;

import java.util.*;

@Service("taxonomyServiceImpl")
public class TaxonomyServiceImpl implements WfServiceHandler {

    Logger logger = LogManager.getLogger(TaxonomyServiceImpl.class);

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    private RequestServiceImpl requestService;

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

    @Override
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest) && !StringUtils.isEmpty(wfRequest)){
            String state = wfRequest.getState();
            RequestTerm term = new RequestTerm();
            HashMap<String, Object> requestMap = new HashMap<>();
            if (state.equals(Constants.INITIATE)){
                requestMap.put(Constants.APPROVAL_STATUS, draft);
            } else if (state.equals(Constants.SEND_FOR_REVIEW_LEVEL_1)){
                requestMap.put(Constants.APPROVAL_STATUS, under_L1_Review);
            } else if (state.equals(Constants.SEND_FOR_REVIEW_LEVEL_2)){
                requestMap.put(Constants.APPROVAL_STATUS, under_L2_Review);
            } else if (state.equals(Constants.APPROVED)){
                requestMap.put(Constants.APPROVAL_STATUS, approved);
            }

            List <HashMap<String, Object>> updateFieldValues =  wfRequest.getUpdateFieldValues();
            if (Objects.nonNull(updateFieldValues) && !CollectionUtils.isEmpty(updateFieldValues)) {
                Request request = new Request();
                for (HashMap<String, Object> updateFieldValue : updateFieldValues) {
                    String identifier = (String) updateFieldValue.get(Constants.IDENTIFIER);
                    String id = (String) updateFieldValue.get(Constants.CODE);
                    String category = (String) updateFieldValue.get(Constants.CATEGORY);
                    requestMap.put(Constants.IDENTIFIER, identifier);
                    term.setTerm(requestMap);
                    request.setRequest(term);
                    String URI = constructTermUpdateURI(id, frameworkId, category);
                    logger.info("printing URI For Term Update {} ", URI);
                    requestService.fetchResultUsingPatch(URI,request, null);
                }
                StringBuilder frameworkURI = constructPublishFrameworkURI(frameworkId);
                HashMap<String, String> headers = new HashMap<>();
                headers.put(Constants.XCHANNELID,channelId);
                requestService.fetchResultUsingPost(frameworkURI,null,Map.class,headers);
            }
        }

    }

    private String constructTermUpdateURI(String term,String framework, String category) {
       String uri = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(framework) && !StringUtils.isEmpty(category)){
            UriComponents  uriComponents = UriComponentsBuilder.fromUriString(host + TERM_UPDATE_URI.replace("{id}", term)).
                    queryParam("framework", framework).queryParam("category", category).build();
           uri = uriComponents.toString();
        }
        return uri;
    }

    private StringBuilder constructPublishFrameworkURI(String framework) {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(framework)){
            builder = new StringBuilder();
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + PUBLISH_FRAMEWORK_URI.replace("{id}",framework)).build();
            builder.append(uriComponents);
        }
        return builder;
    }
}
