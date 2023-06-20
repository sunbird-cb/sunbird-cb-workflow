package org.sunbird.workflow.service.impl;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.BPWorkFlowService;
import org.sunbird.workflow.service.Workflowservice;
import org.sunbird.workflow.utils.CassandraOperation;

import java.util.*;

@Service
public class BPWorkFlowServiceImpl implements BPWorkFlowService {

    @Autowired
    private Workflowservice workflowService;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Override
    public Response enrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response updateBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response readBPWFApplication(String rootOrg, String org, String wfId, String applicationId) {
        Response response = workflowService.getWfApplication(rootOrg, org, wfId, applicationId);
        return response;
    }

    @Override
    public Response blendedProgramSearch(String rootOrg, String org, SearchCriteria criteria) {
        Response response = workflowService.applicationsSearch(rootOrg, org, criteria, Constants.BLENDED_PROGRAM_SEARCH_ENABLED);
        return response;
    }

    @Override
    public Response updateEnrolmentDetails(WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        if (enrolAccess) {

        }
        return null;
    }

  /*  private int getCurrentBatchSize(String batchId, String courseId){
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.ID, batchId);
        propertyMap.put(Constants.COURSE_ID, courseId);
        List<Map<String ,Object>> batchAttributesDetails = cassandraOperation.getRecordsByProperties(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_COURSE_BATCH, propertyMap, Arrays.asList(Constants.BATCH_ATTRIBUTES,Constants.ENROLMENT_END_DATE));
        return Optional.ofNullable(batchAttributesDetails)
                .map(details -> details.get(0))
                .map(details -> details.get(Constants.CURRENT_BATCH_SIZE))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(0);
    }*/

    private Map<String, Object> getCurrentBatchAttributes(String batchId, String courseId) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.BATCH_ID, batchId);
        propertyMap.put(Constants.COURSE_ID, courseId);
        List<Map<String, Object>> batchAttributesDetails = cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_COURSE_BATCH,
                propertyMap,
                Arrays.asList(Constants.BATCH_ATTRIBUTES, Constants.ENROLMENT_END_DATE)
        );
        return batchAttributesDetails.stream()
                .findFirst()
                .map(details -> {
                    Map<String, Object> batchAttributes = new HashMap<>();
                    batchAttributes.put("currentBatchSize", details.getOrDefault(Constants.CURRENT_BATCH_SIZE, 0));
                    batchAttributes.put("enrollmentEndDate", details.getOrDefault(Constants.ENROLMENT_END_DATE, ""));
                    return batchAttributes;
                })
                .orElse(Collections.emptyMap());
    }


    private int getTotalUserEnrolCount(WfRequest wfRequest) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.BATCH_ID, wfRequest.getApplicationId());
        propertyMap.put(Constants.COURSE_ID, wfRequest.getCourseId());
        propertyMap.put(Constants.ENROLLED_DATE, new Date());
        List<Map<String,Object>> batchAttributesDetails = cassandraOperation.getRecordsByProperties(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_ENROLMENT_BATCH_LOOKUP, propertyMap, null);
        return batchAttributesDetails.size();
    }

    private boolean validateBatchEnrolment(Map<String,Object> courseBatchDetails, int totalUserEnrolCount) {
        int currentBatchSize = 0;
        Date enrollmentEndDate = new Date();
        if (MapUtils.isNotEmpty(courseBatchDetails)) {
            currentBatchSize = (int) courseBatchDetails.get("currentBatchSize");
            enrollmentEndDate = (Date) courseBatchDetails.get("enrollmentenddate");
        }
        boolean enrolAccess = totalUserEnrolCount <= currentBatchSize || enrollmentEndDate.before(new Date());
        return enrolAccess;
    }


    @Override
    public Response blendedProgramUserSearch(String rootOrg, String org, String userId, SearchCriteria searchCriteria) {
        searchCriteria.setUserId(userId);
        Response response = workflowService.applicationsSearch(rootOrg, org, searchCriteria, Constants.BLENDED_PROGRAM_SEARCH_ENABLED);
        return response;
    }

}
