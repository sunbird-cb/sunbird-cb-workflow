package org.sunbird.workflow.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.BPWorkFlowService;
import org.sunbird.workflow.service.Workflowservice;
import org.sunbird.workflow.utils.CassandraOperation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BPWorkFlowServiceImpl implements BPWorkFlowService {

    private Logger logger = LoggerFactory.getLogger(BPWorkFlowServiceImpl.class);

    @Autowired
    private Workflowservice workflowService;

    @Autowired
    private CassandraOperation cassandraOperation;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

    @Autowired
    private Configuration configuration;

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Override
    public Response enrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(),
                wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCountForBatch(wfRequest.getApplicationId());
        int totalApprovedUserCount = getTotalApprovedUserCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalApprovedUserCount, totalUserEnrolCount,
                Constants.BP_ENROLL_STATE);
        if (!enrolAccess) {
            Response response = new Response();
            response.put(Constants.ERROR_MESSAGE, "BATCH_IS_FULL");
            response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }
        Response response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        return response;
    }

    @Override
    public Response updateBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        if (validateBatchUserRequestAccess(wfRequest)) {
            return workflowService.workflowTransition(rootOrg, org, wfRequest);
        }
        Response response = new Response();
        response.put(Constants.ERROR_MESSAGE, "BATCH_IS_FULL");
        response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
        return response;
    }

    @Override
    public Response readBPWFApplication(String rootOrg, String org, String wfId, String applicationId) {
        Response response = workflowService.getWfApplication(rootOrg, org, wfId, applicationId);
        return response;
    }

    @Override
    public Response blendedProgramSearch(String rootOrg, String org, SearchCriteria criteria) {
        Response response = workflowService.applicationsSearch(rootOrg, org, criteria,
                Constants.BLENDED_PROGRAM_SEARCH_ENABLED);
        return response;
    }

    @Override
    public void updateEnrolmentDetails(WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(),
                wfRequest.getCourseId());
        int totalApprovedUserCount = getTotalApprovedUserCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalApprovedUserCount, 0,
                Constants.BP_UPDATE_STATE);
        if (enrolAccess) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(Constants.USER_ID, wfRequest.getUserId());
            requestBody.put(Constants.BATCH_ID, wfRequest.getApplicationId());
            requestBody.put(Constants.COURSE_ID, wfRequest.getCourseId());
            Map<String, Object> request = new HashMap<>();
            request.put(Constants.REQUEST, requestBody);
            HashMap<String, String> headersValue = new HashMap<>();
            headersValue.put("Content-Type", "application/json");
            try {
                StringBuilder builder = new StringBuilder(configuration.getCourseServiceHost());
                builder.append(configuration.getAdminEnrolEndPoint());
                Map<String, Object> enrolResp = (Map<String, Object>) requestServiceImpl
                        .fetchResultUsingPost(builder, request, Map.class, headersValue);
                if (enrolResp != null
                        && "OK".equalsIgnoreCase((String) enrolResp.get(Constants.RESPONSE_CODE))) {
                    logger.info("User enrolment success");
                } else {
                    logger.error("user enrolment failed"
                            + ((Map<String, Object>) enrolResp.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
                }
            } catch (Exception e) {
                logger.error("Exception while enrol user");
            }
        }
    }

    private Map<String, Object> getCurrentBatchAttributes(String batchId, String courseId) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.BATCH_ID, batchId);
        propertyMap.put(Constants.COURSE_ID, courseId);
        List<Map<String, Object>> batchAttributesDetails = cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_COURSE_BATCH,
                propertyMap,
                Arrays.asList(Constants.BATCH_ATTRIBUTES, Constants.ENROLMENT_END_DATE));
        if (CollectionUtils.isNotEmpty(batchAttributesDetails)) {
            Map<String, Object> courseBatch = (Map<String, Object>) batchAttributesDetails.get(0);
            if (courseBatch.containsKey(Constants.BATCH_ATTRIBUTES)) {
                try {
                    Map<String, Object> batchAttributes = (new ObjectMapper()).readValue(
                            (String) courseBatch.get(Constants.BATCH_ATTRIBUTES),
                            new TypeReference<HashMap<String, Object>>() {
                            });

                    String currentBatchSizeString = batchAttributes != null
                            && batchAttributes.containsKey(Constants.CURRENT_BATCH_SIZE)
                                    ? (String) batchAttributes.get(Constants.CURRENT_BATCH_SIZE)
                                    : "0";
                    int currentBatchSize = Integer.parseInt(currentBatchSizeString);
                    Date enrollmentEndDate = courseBatch.containsKey(Constants.ENROLMENT_END_DATE)
                            ? (Date) courseBatch.get(Constants.ENROLMENT_END_DATE)
                            : null;
                    Map<String, Object> result = new HashMap<>();
                    result.put(Constants.CURRENT_BATCH_SIZE, currentBatchSize);
                    result.put(Constants.ENROLMENT_END_DATE, enrollmentEndDate);
                    return result;
                } catch (Exception e) {
                    logger.error(String.format("Failed to retrieve course batch details. CourseId: %s, BatchId: %s",
                            courseId, batchId), e);
                }
            }
        }
        return Collections.emptyMap();
    }

    private int getTotalApprovedUserCount(WfRequest wfRequest) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.BATCH_ID, wfRequest.getApplicationId());
        int totalCount = cassandraOperation.getCountByProperties(Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_ENROLMENT_BATCH_LOOKUP, propertyMap);
        return totalCount;
    }

    private boolean validateBatchEnrolment(Map<String, Object> courseBatchDetails, int totalApprovedUserCount,
            int totalUserEnrolCount, String bpState) {
        if (MapUtils.isEmpty(courseBatchDetails)) {
            return false;
        }
        int currentBatchSize = 0;
        if (courseBatchDetails.containsKey(Constants.CURRENT_BATCH_SIZE)) {
            currentBatchSize = (int) courseBatchDetails.get(Constants.CURRENT_BATCH_SIZE);
        }
        if (totalApprovedUserCount >= currentBatchSize) {
            return false;
        }
        Date enrollmentEndDate = (Date) courseBatchDetails.get(Constants.ENROLMENT_END_DATE);
        if (currentBatchSize != 0 && Constants.BP_ENROLL_STATE.equals(bpState)) {
            currentBatchSize = (int) Math.round(currentBatchSize
                    + (((double) configuration.getBpBatchEnrolLimitBufferSize() / 100) * currentBatchSize));
        } else {
            totalUserEnrolCount = totalApprovedUserCount;
        }
        boolean enrolAccess = (totalUserEnrolCount < currentBatchSize) && (enrollmentEndDate.after(new Date()));
        return enrolAccess;
    }

    @Override
    public Response blendedProgramUserSearch(String rootOrg, String org, String userId, SearchCriteria searchCriteria) {
        searchCriteria.setUserId(userId);
        Response response = workflowService.applicationsSearch(rootOrg, org, searchCriteria);
        return response;
    }

    public Response readBPWFApplication(String wfId, boolean isPc) {
        WfStatusEntity applicationStatus = wfStatusRepo.findByWfId(wfId);
        List<WfStatusEntity> applicationList = applicationStatus == null ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(applicationStatus));
        Response response = new Response();
        if (isPc) {
            // TODO - Need to enrich this response with User Profile Details ?
        }
        response.put(Constants.MESSAGE, Constants.SUCCESSFUL);
        response.put(Constants.DATA, applicationList);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     * This method is responsible for processing the wfRequest based on the state of
     * the wfRequest
     *
     * @param wfRequest - Recieves a wfRequest with the request params.
     */
    public void processWFRequest(WfRequest wfRequest) {
        WfStatusEntity wfStatusEntity = wfStatusRepo.findByWfId(wfRequest.getWfId());
        switch (wfStatusEntity.getCurrentStatus()) {
            case Constants.APPROVED:
                updateEnrolmentDetails(wfRequest);
                break;
            default:
                logger.info("Status is Skipped by Blended Program Workflow Handler - Current Status: "
                        + wfStatusEntity.getCurrentStatus());
                break;
        }
    }

    private boolean validateBatchUserRequestAccess(WfRequest wfRequest) {
        if (configuration.getBpBatchFullValidationExcludeStates().contains(wfRequest.getAction())) {
            return true;
        }
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(),
                wfRequest.getCourseId());
        return validateBatchEnrolment(courseBatchDetails, getTotalApprovedUserCount(wfRequest), 0,
                Constants.BP_UPDATE_STATE);
    }

    public Response readStats(Map<String, Object> request) {
        Response response = new Response();
        try {
            String errMsg = validateRequest(request);
            if (StringUtils.isNotBlank(errMsg)) {
                response.put(Constants.MESSAGE, Constants.FAILED);
                response.put(Constants.ERROR_MESSAGE, errMsg);
                response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
                response.setResponseCode(HttpStatus.BAD_REQUEST);
                return response;
            }
            Map<String, Object> respCourseDetails = new HashMap<String, Object>();
            Map<String, Object> requestBody = (Map<String, Object>) request.get(Constants.REQUEST);
            List<String> courseIdList = (List<String>) requestBody.get(Constants.COURSE_ID_LIST);
            List<String> fileds = Arrays.asList(Constants.COURSE_ID, Constants.BATCH_ID, Constants.END_DATE);
            Map<String, Object> propertyMap = new HashMap<String, Object>();
            propertyMap.put(Constants.COURSE_ID, courseIdList);
            List<Map<String, Object>> courseBatchDetailsList = cassandraOperation.getRecordsByProperties(
                    Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_COURSE_BATCH, propertyMap, fileds);
            for (Map<String, Object> courseBatchDetail : courseBatchDetailsList) {
                // Process batch details
                Date endDate = courseBatchDetail.containsKey(Constants.END_DATE)
                        ? (Date) courseBatchDetail.get(Constants.END_DATE)
                        : null;
                Date todayDate = new Date();
                if (todayDate.after(endDate)) {
                    continue;
                }
                String courseId = (String) courseBatchDetail.get(Constants.COURSE_ID_KEY);
                String batchId = (String) courseBatchDetail.get(Constants.BATCH_ID_KEY);
                Map<String, Object> respCourseBatch = (Map<String, Object>) respCourseDetails.get(courseId);
                Map<String, Object> respBatchDetailsMap = null;
                if (ObjectUtils.isEmpty(respCourseBatch)) {
                    respCourseBatch = new HashMap<String, Object>();
                    respCourseBatch.put(Constants.COURSE_ID, courseId);
                    respBatchDetailsMap = new HashMap<String, Object>();
                } else {
                    respBatchDetailsMap = (Map<String, Object>) respCourseBatch.get(Constants.BATCH_DETAILS_KEY);
                }

                Map<String, Object> respBatchDetail = new HashMap<String, Object>();
                // Batch is active. Get the batch enrolment details.
                List<WfStatusEntity> wfEntries = wfStatusRepo.findByServiceNameAndApplicationId(
                        Constants.BLENDED_PROGRAM_SERVICE_NAME, batchId);
                Map<String, Long> statusCount = wfEntries.stream()
                        .map(WfStatusEntity::getCurrentStatus)
                        .collect(Collectors.groupingBy(status -> status, Collectors.counting()));

                long newRequestCount = 0;
                long learnerCount = 0;
                long rejectedCount = 0;
                for (Map.Entry<String, Long> entry : statusCount.entrySet()) {
                    switch (entry.getKey()) {
                        case Constants.SEND_FOR_PC_APPROVAL:
                            newRequestCount = entry.getValue();
                            break;
                        case Constants.REJECTED:
                            rejectedCount = entry.getValue();
                            break;
                        case Constants.APPROVED:
                            learnerCount = entry.getValue();
                            break;
                        default:
                            break;
                    }
                }
                respBatchDetail.put(Constants.NEW_REQUEST_COUNT, newRequestCount);
                respBatchDetail.put(Constants.LEARNER_COUNT, learnerCount);
                respBatchDetail.put(Constants.REJECTED_COUNT, rejectedCount);

                respCourseBatch.put(Constants.TOTAL_LEARNER_COUNT,
                        ((long) respCourseBatch.getOrDefault(Constants.TOTAL_LEARNER_COUNT, 0l)) + learnerCount);
                respCourseBatch.put(Constants.TOTAL_NEW_REQUEST_COUNT,
                        ((long) respCourseBatch.getOrDefault(Constants.TOTAL_NEW_REQUEST_COUNT, 0l)) + newRequestCount);
                respCourseBatch.put(Constants.TOTAL_REJECTED_COUNT,
                        ((long) respCourseBatch.getOrDefault(Constants.TOTAL_REJECTED_COUNT, 0l)) + rejectedCount);
                respBatchDetailsMap.put(batchId, respBatchDetail);
                respCourseBatch.put(Constants.BATCH_DETAILS_KEY, respBatchDetailsMap);
                respCourseDetails.put(courseId, respCourseBatch);
            }
            response.setResponseCode(HttpStatus.OK);
            response.getResult().put(Constants.COUNT, respCourseDetails.size());
            response.getResult().put(Constants.CONTENT, respCourseDetails.values());
        } catch (Exception e) {
            String errMsg = String.format("Failed to get the stats for course. Exception: ", e.getMessage());
            response.put(Constants.ERROR_MESSAGE, errMsg);
            response.put(Constants.STATUS, HttpStatus.INTERNAL_SERVER_ERROR);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            logger.error(errMsg, e);
        }
        return response;
    }

    private String validateRequest(Map<String, Object> request) {
        String errMsg = "";
        Map<String, Object> requestBody = (Map<String, Object>) request.get(Constants.REQUEST);
        if (ObjectUtils.isEmpty(requestBody)) {
            errMsg = "Invalid Request";
            return errMsg;
        }

        if (ObjectUtils.isEmpty(((List<String>) requestBody.get(Constants.COURSE_ID_LIST)))) {
            errMsg = "Invalid Request. CourseIdList is empty.";
        }
        return errMsg;
    }

    private int getTotalUserEnrolCountForBatch(String applicationId) {
        List<WfStatusEntity> wfEntries = wfStatusRepo
                .findByServiceNameAndApplicationId(Constants.BLENDED_PROGRAM_SERVICE_NAME, applicationId);
        wfEntries = wfEntries.stream().filter(wfEntry -> !configuration.getBpBatchFullValidationExcludeStates()
                .contains(wfEntry.getCurrentStatus()))
                .collect(Collectors.toList());
        return wfEntries.size();
    }
}
