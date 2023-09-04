package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.InvalidDataInputException;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.producer.Producer;
import org.sunbird.workflow.service.BPWorkFlowService;
import org.sunbird.workflow.service.Workflowservice;
import org.sunbird.workflow.utils.CassandraOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;

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

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private Producer producer;

    @Override
    public Response enrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        if (!enrolAccess) {
            Response response = new Response();
            response.put(Constants.ERROR_MESSAGE, "BATCH_IS_FULL");
            response.put(Constants.STATUS,HttpStatus.BAD_REQUEST);
            return response;
        }
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
    public void updateEnrolmentDetails(WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        if (enrolAccess) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(Constants.USER_ID, wfRequest.getUserId());
            requestBody.put(Constants.BATCH_ID, wfRequest.getApplicationId());
            requestBody.put(Constants.COURSE_ID, wfRequest.getCourseId());
            Map<String, Object> request = new HashMap<>();
            request.put(Constants.REQUEST,requestBody);
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
                    logger.error("user enrolment failed" + ((Map<String, Object>) enrolResp.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
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


    private int getTotalUserEnrolCount(WfRequest wfRequest) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.BATCH_ID, wfRequest.getApplicationId());
        int totalCount = cassandraOperation.getCountByProperties(Constants.KEYSPACE_SUNBIRD_COURSES, Constants.TABLE_ENROLMENT_BATCH_LOOKUP, propertyMap);
        return totalCount;
    }

    private boolean validateBatchEnrolment(Map<String, Object> courseBatchDetails, int totalUserEnrolCount) {
        if (MapUtils.isEmpty(courseBatchDetails)) {
            return false;
        }
        int currentBatchSize = 0;
        if (courseBatchDetails.containsKey(Constants.CURRENT_BATCH_SIZE)) {
            currentBatchSize = (int) courseBatchDetails.get(Constants.CURRENT_BATCH_SIZE);
        }
        Date enrollmentEndDate = (Date) courseBatchDetails.get(Constants.ENROLMENT_END_DATE);

        boolean enrolAccess = (totalUserEnrolCount + 1 <= currentBatchSize) && (enrollmentEndDate.after(new Date()));
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
     * Service method to handle the user enrolled by the admin.
     *
     * @param rootOrg   - Root Organization Name ex: "igot"
     * @param org       - Organization name ex: "dopt"
     * @param wfRequest - WorkFlow request which needs to be processed.
     * @return - Return the response of success/failure after processing the request.
     */
    @Override
    public Response adminEnrolBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        if (!enrolAccess) {
            Response response = new Response();
            response.put(Constants.ERROR_MESSAGE, "BATCH_IS_FULL");
            response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
            return response;
        }
        Response response;
        if (!scheduleConflictCheck(wfRequest)) {
            List<WfStatusEntity> enrollmentStatus = wfStatusRepo.findByServiceNameAndUserIdAndApplicationId(wfRequest.getServiceName(), wfRequest.getUserId(), wfRequest.getApplicationId());

            if (!enrollmentStatus.isEmpty()) {
                response = new Response();
                response.put(Constants.MESSAGE, "Not allowed to enroll the user to the Blended Program");
                response.put(Constants.STATUS, HttpStatus.OK);
            } else {
                response = saveAdminEnrollUserIntoWfStatus(rootOrg, org, wfRequest);
                producer.push(configuration.getWorkFlowNotificationTopic(), wfRequest);
                producer.push(configuration.getWorkflowApplicationTopic(), wfRequest);
            }
        } else {
            response = new Response();
            response.put(Constants.MESSAGE, "Not allowed to enroll the user to the Blended Program since there is a schedule conflict");
            response.put(Constants.STATUS, HttpStatus.OK);
        }
        return response;
    }

    /**
     * Save Method to save the admin enrolled data into the wf_status table.
     *
     * @param rootOrg   - Root Organization Name ex: "igot"
     * @param org       - Organization name ex: "dopt"
     * @param wfRequest - WorkFlow request which needs to be processed.
     * @return - Return the response of success/failure after processing the request.
     */
    private Response saveAdminEnrollUserIntoWfStatus(String rootOrg, String org, WfRequest wfRequest) {
        validateWfRequest(wfRequest);
        WfStatusEntity applicationStatus = new WfStatusEntity();
        String wfId = UUID.randomUUID().toString();
        applicationStatus.setWfId(wfId);
        applicationStatus.setApplicationId(wfRequest.getApplicationId());
        applicationStatus.setUserId(wfRequest.getUserId());
        applicationStatus.setInWorkflow(true);
        applicationStatus.setServiceName(wfRequest.getServiceName());
        applicationStatus.setActorUUID(wfRequest.getActorUserId());
        applicationStatus.setCreatedOn(new Date());
        applicationStatus.setCurrentStatus(Constants.APPROVED_STATE);
        applicationStatus.setLastUpdatedOn(new Date());
        applicationStatus.setOrg(org);
        applicationStatus.setRootOrg(rootOrg);
        try {
            applicationStatus.setUpdateFieldValues(mapper.writeValueAsString(wfRequest.getUpdateFieldValues()));
        } catch (JsonProcessingException e) {
            logger.error(String.valueOf(e));
        }
        applicationStatus.setDeptName(wfRequest.getDeptName());
        applicationStatus.setComment(wfRequest.getComment());
        wfRequest.setWfId(wfId);
        wfStatusRepo.save(applicationStatus);

        Response response = new Response();
        HashMap<String, Object> data = new HashMap<>();
        data.put(Constants.STATUS, Constants.APPROVED_STATE);
        data.put(Constants.WF_IDS_CONSTANT, wfId);
        response.put(Constants.MESSAGE, Constants.STATUS_CHANGE_MESSAGE + Constants.APPROVED_STATE);
        response.put(Constants.DATA, data);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     * @param wfRequest - Validate the fields received in the wfRequest.
     */
    private void validateWfRequest(WfRequest wfRequest) {

        if (StringUtils.isEmpty(wfRequest.getState())) {
            throw new InvalidDataInputException(Constants.STATE_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getApplicationId())) {
            throw new InvalidDataInputException(Constants.APPLICATION_ID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getActorUserId())) {
            throw new InvalidDataInputException(Constants.ACTOR_UUID_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getUserId())) {
            throw new InvalidDataInputException(Constants.USER_UUID_VALIDATION_ERROR);
        }


        if (CollectionUtils.isEmpty(wfRequest.getUpdateFieldValues())) {
            throw new InvalidDataInputException(Constants.FIELD_VALUE_VALIDATION_ERROR);
        }

        if (StringUtils.isEmpty(wfRequest.getServiceName())) {
            throw new InvalidDataInputException(Constants.WORKFLOW_SERVICENAME_VALIDATION_ERROR);
        }
    }

    /**
     * This method is responsible for processing the wfRequest based on the state of the wfRequest
     *
     * @param wfRequest - Recieves a wfRequest with the request params.
     */
    public void processWFRequest(WfRequest wfRequest) {
        WfStatusEntity wfStatusEntity = wfStatusRepo.findByWfId(wfRequest.getWfId());
        switch (wfStatusEntity.getCurrentStatus()) {
            case Constants.APPROVED:
                updateEnrolmentDetails(wfRequest);
                break;
            case Constants.REMOVED:
                removeEnrolmentDetails(wfRequest);
                break;
            default:
                logger.info("Status is Skipped by Blended Program Workflow Handler - Current Status: " + wfStatusEntity.getCurrentStatus());
                break;
        }
    }

    /**
     * Main method is responsible for checking the schedule conflicts wrt enrollment of user into blended program.
     *
     * @param wfRequest - WorkFlow request which needs to be processed.
     * @return - return the response of success/failure after processing the request.
     */
    public boolean scheduleConflictCheck(WfRequest wfRequest) {
        final Date[] wfBatchStartDate = new Date[1];
        final Date[] wfBatchEndDate = new Date[1];
        List<Map<String, Object>> userEnrollmentBatchDetailsList = getUserEnrolmentDetails(wfRequest);
        List<Map<String, Object>> courseBatchWfRequestList = getCourseBatchDetailWfRequest(wfRequest);
        List<Map<String, Object>> enrolledCourseBatchList = getCourseBatchDetails(userEnrollmentBatchDetailsList);
        courseBatchWfRequestList.stream().flatMap(courseBatchWfRequest -> courseBatchWfRequest.entrySet().stream()).forEach(entry -> {
            if (entry.getKey().equals(Constants.START_DATE)) {
                Date startDate = (Date) entry.getValue();
                if (startDate != null) {
                    wfBatchStartDate[0] = startDate;
                }
            }
            if (entry.getKey().equals(Constants.END_DATE)) {
                Date endDate = (Date) entry.getValue();
                if (endDate != null) {
                    wfBatchEndDate[0] = endDate;
                }
            }
        });
        return enrollmentDateValidations(enrolledCourseBatchList, wfBatchStartDate, wfBatchEndDate);
    }

    /**
     * This method is responsible  for checking the date conflicts of the blended program
     * received from wfRequest with the blended programs the user is already enrolled into.
     *
     * @param enrolledCourseBatchList - contains details of the enrolled courses for the user.
     * @param startDate               - startDate for the course received from the wfRequest.
     * @param endDate-                endDate for the course received from the wfRequest.
     * @return - return a boolean value 'true' is there is conflict of the dates.
     */
    public boolean enrollmentDateValidations(List<Map<String, Object>> enrolledCourseBatchList, Date[] startDate, Date[] endDate) {
        final boolean[] startDateFlag = {false};
        final boolean[] endDateFlag = {false};
        enrolledCourseBatchList.forEach(enrolledCourseBatch -> enrolledCourseBatch.forEach((key, value) -> {
            Date startDateValue = (Date) enrolledCourseBatch.get(Constants.START_DATE);
            Date endDateValue = (Date) enrolledCourseBatch.get(Constants.END_DATE);
            if (startDateValue != null && isWithinRange(startDateValue, startDate[0], endDate[0])) {
                logger.info("The user is not allowed to enroll for the course since there is a conflict" + startDateValue + startDate[0] + endDate[0]);
                startDateFlag[0] = true;
            } else {
                startDateFlag[0] = false;
            }

            if (endDateValue != null && isWithinRange(endDateValue, startDate[0], endDate[0])) {
                logger.info("The user is not allowed to enroll for the course since there is a conflict" + endDateValue + startDate[0] + endDate[0]);
                endDateFlag[0] = true;
            } else {
                endDateFlag[0] = false;
            }
        }));
        return startDateFlag[0] || endDateFlag[0];
    }

    /**
     * This method returns the list of courses the user is enrolled into.
     *
     * @param wfRequest - WorkFlow request which contains the parameters.
     * @return - return a list of the user_enrolment details based on the userid passed.
     */
    public List<Map<String, Object>> getUserEnrolmentDetails(WfRequest wfRequest) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.USER_ID, wfRequest.getUserId());
        return cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.USER_ENROLMENTS,
                propertyMap,
                Arrays.asList(Constants.BATCH_ID, Constants.USER_ID, Constants.COURSE_ID)
        );
    }

    /**
     * This method returns the course_batch details for the blended program received from the wfRequest.
     *
     * @param wfRequest -  WorkFlow request which contains the parameters.
     * @return - return a list of the course_batch details based on the courseId and batchId passed.
     */
    public List<Map<String, Object>> getCourseBatchDetailWfRequest(WfRequest wfRequest) {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.COURSE_ID, wfRequest.getCourseId());
        propertyMap.put(Constants.BATCH_ID, wfRequest.getApplicationId());
        return cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_COURSE_BATCH,
                propertyMap,
                Arrays.asList(Constants.BATCH_ID, Constants.COURSE_ID, Constants.START_DATE, Constants.END_DATE)
        );
    }

    /**
     * This method returns the course_batch details for the blended program based on the user_enrolment details.
     *
     * @param userEnrollmentBatchDetailsList - To get the course batch details we need the user_enrolment table details specifically - courseId and batchId
     * @return - return a list of the course_batch details based on the courseId and batchId passed.
     */
    public List<Map<String, Object>> getCourseBatchDetails(List<Map<String, Object>> userEnrollmentBatchDetailsList) {
        List<String> coursesList = new ArrayList<>();
        List<String> batchidsList = new ArrayList<>();
        userEnrollmentBatchDetailsList.forEach(userEnrollmentBatchDetail ->
                userEnrollmentBatchDetail.entrySet().stream()
                        .filter(entry -> entry.getKey().equalsIgnoreCase(Constants.COURSE_ID) ||
                                entry.getKey().equalsIgnoreCase(Constants.BATCH_ID))
                        .forEach(entry -> {
                            if (entry.getKey().equalsIgnoreCase(Constants.COURSE_ID)) {
                                coursesList.add(entry.getValue().toString());
                            }
                            if (entry.getKey().equalsIgnoreCase(Constants.BATCH_ID)) {
                                batchidsList.add(entry.getValue().toString());
                            }
                        })
        );

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(Constants.COURSE_ID, coursesList);
        propertyMap.put(Constants.BATCH_ID, batchidsList);
        propertyMap.put(Constants.ENROLLMENT_TYPE, Constants.INVITE_ONLY);
        return cassandraOperation.getRecordsByProperties(
                Constants.KEYSPACE_SUNBIRD_COURSES,
                Constants.TABLE_COURSE_BATCH,
                propertyMap,
                Arrays.asList(Constants.BATCH_ID, Constants.COURSE_ID, Constants.START_DATE, Constants.END_DATE)
        );
    }

    /**
     * This method is responsible to check the date in a specific range.
     *
     * @param date        - The needs to be checked whether it is in the range.
     * @param startDate   -The startDate wrt to the Blended program to be enrolled in.
     * @param endDate-The endDate wrt to the Blended program to be enrolled in.
     * @return - Boolean value if the date is in the range of the Blended program enrollment.
     */
    public static boolean isWithinRange(Date date, Date startDate, Date endDate) {
        return date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0;
    }

    @Override
    public Response removeBPWorkFlow(String rootOrg, String org, WfRequest wfRequest) {
        Response response = new Response();
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        List<WfStatusEntity> approvedLearners = wfStatusRepo.findByApplicationIdAndUserIdAndCurrentStatus(wfRequest.getApplicationId(), wfRequest.getUserId(), wfRequest.getState());
        if (enrolAccess && approvedLearners.size() > 1) {
            response.put(Constants.ERROR_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (approvedLearners.size() == 1)
            wfRequest.setWfId(approvedLearners.get(0).getWfId());
        response = workflowService.workflowTransition(rootOrg, org, wfRequest);
        response.put(Constants.STATUS, HttpStatus.OK);
        return response;
    }

    /**
     * This method is responsible for removing a user enrollment details
     *
     * @param wfRequest - Receives a wfRequest with the request params.
     */
    @Override
    public void removeEnrolmentDetails(WfRequest wfRequest) {
        Map<String, Object> courseBatchDetails = getCurrentBatchAttributes(wfRequest.getApplicationId(), wfRequest.getCourseId());
        int totalUserEnrolCount = getTotalUserEnrolCount(wfRequest);
        boolean enrolAccess = validateBatchEnrolment(courseBatchDetails, totalUserEnrolCount);
        if (enrolAccess) {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(Constants.USER_ID, wfRequest.getUserId());
            requestBody.put(Constants.BATCH_ID, wfRequest.getApplicationId());
            requestBody.put(Constants.COURSE_ID, wfRequest.getCourseId());
            Map<String, Object> request = new HashMap<>();
            request.put(Constants.REQUEST,requestBody);
            HashMap<String, String> headersValue = new HashMap<>();
            headersValue.put("Content-Type", "application/json");
            try {
                StringBuilder builder = new StringBuilder(configuration.getCourseServiceHost());
                builder.append(configuration.getAdminUnEnrolEndPoint());
                Map<String, Object> enrolResp = (Map<String, Object>) requestServiceImpl
                        .fetchResultUsingPost(builder, request, Map.class, headersValue);
                if (enrolResp != null
                        && "OK".equalsIgnoreCase((String) enrolResp.get(Constants.RESPONSE_CODE))) {
                    logger.info("User un-enrollment success");
                } else {
                    logger.error("user un-enrollment failed" + ((Map<String, Object>) enrolResp.get(Constants.PARAMS)).get(Constants.ERROR_MESSAGE));
                }
            } catch (Exception e) {
                logger.error("Exception while un-enrol user");
            }
        }
    }

}
