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
import org.sunbird.workflow.models.Request;
import org.sunbird.workflow.models.RequestTerm;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
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

    @Override
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest) && !StringUtils.isEmpty(wfRequest)){
            String state = wfRequest.getState();
            HashMap<String, Object> request = new HashMap<>();
            HashMap<String, Object> term = new HashMap<>();
            HashMap<String, Object> requestMap = new HashMap<>();
            List <HashMap<String, Object>> updateFieldValues = null;
            if (state.equals(Constants.INITIATE)){
                requestMap.put(Constants.APPROVAL_STATUS, draft);
            } else if (state.equals(Constants.SEND_FOR_REVIEW_LEVEL_1)){
                requestMap.put(Constants.APPROVAL_STATUS, under_L1_Review);
            } else if (state.equals(Constants.SEND_FOR_REVIEW_LEVEL_2)){
                requestMap.put(Constants.APPROVAL_STATUS, under_L2_Review);
            } else if (state.equals(Constants.APPROVED)){
                requestMap.put(Constants.APPROVAL_STATUS, approved);
            }

            if (state.equals(Constants.INITIATE)){
                updateFieldValues  =  wfRequest.getUpdateFieldValues();
            } else {
                WfStatusEntityV2 wfStatusEntityV2 =  wfStatusRepo.findBywfId(wfRequest.getWfId());
                updateFieldValues = getUpdateFieldValues(wfStatusEntityV2);
                System.out.println("field values :: "+updateFieldValues);
            }

            if (Objects.nonNull(updateFieldValues) && !CollectionUtils.isEmpty(updateFieldValues)) {
                for (HashMap<String, Object> updateFieldValue : updateFieldValues) {
                    String identifier = (String) updateFieldValue.get(Constants.IDENTIFIER);
                    String id = (String) updateFieldValue.get(Constants.CODE);
                    String category = (String) updateFieldValue.get(Constants.CATEGORY);
                    requestMap.put(Constants.IDENTIFIER, identifier);
                    term.put(Constants.TERM,requestMap);
                    request.put(Constants.REQUEST,term);
                    String URI = constructTermUpdateURI(id, category);
                    logger.info("printing URI For Term Update {} ", URI);
                    requestService.fetchResultUsingPatch(URI,request, null);
                }
                StringBuilder frameworkURI = constructPublishFrameworkURI();
                HashMap<String, String> headers = new HashMap<>();
                headers.put(Constants.XCHANNELID,channelId);
                requestService.fetchResultUsingPost(frameworkURI,null,Map.class,headers);
            }
        }

    }

    private String constructTermUpdateURI(String term, String category) {
       String uri = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(frameworkId) && !StringUtils.isEmpty(category)){
            UriComponents  uriComponents = UriComponentsBuilder.fromUriString(host + TERM_UPDATE_URI.replace(Constants.ID, term)).
                    queryParam(Constants.FRAMEWORK, frameworkId).queryParam(Constants.CATEGORY, category).build();
           uri = uriComponents.toString();
        }
        return uri;
    }

    private StringBuilder constructPublishFrameworkURI() {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(frameworkId)){
            builder = new StringBuilder();
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(host + PUBLISH_FRAMEWORK_URI.replace(Constants.ID,frameworkId)).build();
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
}
