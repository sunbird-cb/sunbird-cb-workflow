package org.sunbird.workflow.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.exception.ApplicationException;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.impl.RequestServiceImpl;
import org.sunbird.workflow.utils.CassandraOperation;
import org.sunbird.workflow.utils.ValidationUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UserBulkUploadService {
    private final Logger logger = LoggerFactory.getLogger(UserBulkUploadService.class);
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    Configuration configuration;

    @Autowired
    CassandraOperation cassandraOperation;

    @Autowired
    private RequestServiceImpl requestServiceImpl;

//    @Autowired
//    UserUtilityService userUtilityService;

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    UserProfileWfService userProfileWfService;

    @Autowired
    private ObjectMapper mapper;



    @Autowired
    StorageService storageService;

    public void initiateUserBulkUploadProcess(String inputData) {
        logger.info("UserBulkUploadService:: initiateUserBulkUploadProcess: Started");
        long duration = 0;
        long startTime = System.currentTimeMillis();
        try {
            HashMap<String, String> inputDataMap = objectMapper.readValue(inputData, new TypeReference<HashMap<String, String>>() {});
            this.processBulkUpload(inputDataMap);
            if (null != inputData) {
                this.updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID),
                        inputDataMap.get(Constants.IDENTIFIER), Constants.STATUS_IN_PROGRESS_UPPERCASE,
                        0,
                        0,
                        0);
                storageService.downloadFile(inputDataMap.get(Constants.FILE_NAME));
                this.processBulkUpload(inputDataMap);
            } else {
                logger.error("Error in the Kafka Message Received");
            }
        } catch (Exception e) {
            String errMsg = String.format("Error in the scheduler to upload bulk users %s", e.getMessage());
            logger.error(errMsg, e);
        }
        duration = System.currentTimeMillis() - startTime;
        logger.info("UserBulkUploadService:: initiateUserBulkUploadProcess: Completed. Time taken: {} milli-seconds", duration );
    }

    public void updateUserBulkUploadStatus(String rootOrgId, String identifier, String status, int totalRecordsCount,
                                           int successfulRecordsCount, int failedRecordsCount) {
        try {
            Map<String, Object> compositeKeys = new HashMap<>();
            compositeKeys.put(Constants.ROOT_ORG_ID_LOWER, rootOrgId);
            compositeKeys.put(Constants.IDENTIFIER, identifier);
            Map<String, Object> fieldsToBeUpdated = new HashMap<>();
            if (!status.isEmpty()) {
                fieldsToBeUpdated.put(Constants.STATUS, status);
            }
            if (totalRecordsCount >= 0) {
                fieldsToBeUpdated.put(Constants.TOTAL_RECORDS, totalRecordsCount);
            }
            if (successfulRecordsCount >= 0) {
                fieldsToBeUpdated.put(Constants.SUCCESSFUL_RECORDS_COUNT, successfulRecordsCount);
            }
            if (failedRecordsCount >= 0) {
                fieldsToBeUpdated.put(Constants.FAILED_RECORDS_COUNT, failedRecordsCount);
            }
            fieldsToBeUpdated.put(Constants.DATE_UPDATE_ON, new Timestamp(System.currentTimeMillis()));
            cassandraOperation.updateRecord(Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER_BULK_UPDATE,
                    fieldsToBeUpdated, compositeKeys);
        } catch (Exception e) {
            logger.error(String.format("Error in Updating User Bulk Upload Status in Cassandra %s", e.getMessage()), e);
        }
    }

    private void processBulkUpload(HashMap<String, String> inputDataMap) throws IOException {
        File file = null;
        FileInputStream fis = null;
        XSSFWorkbook wb = null;
        int totalRecordsCount = 0;
        int noOfSuccessfulRecords = 0;
        int failedRecordsCount = 0;
        String status = "";
        try {
            file = new File(Constants.LOCAL_BASE_PATH + inputDataMap.get(Constants.FILE_NAME));
            if (file.exists() && file.length() > 0) {
                fis = new FileInputStream(file);
                wb = new XSSFWorkbook(fis);
                XSSFSheet sheet = wb.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();
                // incrementing the iterator inorder to skip the headers in the first row
                if (rowIterator.hasNext()) {
                    Row firstRow = rowIterator.next();
                    Cell statusCell = firstRow.getCell(10);
                    Cell errorDetails = firstRow.getCell(11);
                    if (statusCell == null) {
                        statusCell = firstRow.createCell(10);
                    }
                    if (errorDetails == null) {
                        errorDetails = firstRow.createCell(11);
                    }
                    statusCell.setCellValue(Constants.STATUS);
                    errorDetails.setCellValue(Constants.ERROR_DETAILS);
                }
                int count = 0;
                while (rowIterator.hasNext()) {
                    logger.info("UserBulkUploadService:: Record {}" , count++);
                    long duration = 0;
                    long startTime = System.currentTimeMillis();
                    StringBuffer str = new StringBuffer();
                    List<String> errList = new ArrayList<>();
                    Row nextRow = rowIterator.next();
                    Map<String, String> valuesToBeUpdate = new HashMap<>();
                    Map<String, Object> userDetails = null;
                    boolean isEmailOrPhoneNumberValid = false;
                    Cell statusCell = nextRow.getCell(10);
                    if (statusCell == null) {
                        statusCell = nextRow.createCell(10);
                    }
                    Cell errorDetails = nextRow.getCell(11);
                    if (errorDetails == null) {
                        errorDetails = nextRow.createCell(11);
                    }
                    if (nextRow.getCell(0) != null || nextRow.getCell(0).getCellType() != CellType.BLANK) {
                        String email = nextRow.getCell(0).getStringCellValue().trim();
                        if(ValidationUtil.validateEmailPattern(email)){
                            userDetails = new HashMap<>();
                            isEmailOrPhoneNumberValid = this.verifyUserRecordExists(Constants.EMAIL, email, userDetails);
                        }
                    }
                    if(!isEmailOrPhoneNumberValid){
                        String phoneNumber = null;
                        boolean isValidPhoneNumber = false;
                        if (nextRow.getCell(1) != null || nextRow.getCell(1).getCellType() != CellType.BLANK) {
                            if (nextRow.getCell(1).getCellType() == CellType.NUMERIC) {
                                phoneNumber = NumberToTextConverter.toText(nextRow.getCell(1).getNumericCellValue());
                            } else if (nextRow.getCell(1).getCellType() == CellType.STRING) {
                                phoneNumber = nextRow.getCell(1).getStringCellValue().trim();
                            } else {
                                errList.add("Invalid column type. Expecting number/string format");
                            }
                        } else {
                            errList.add("Phone Number is Missing");
                        }
                        if(!StringUtils.isEmpty(phoneNumber)){
                            isValidPhoneNumber = ValidationUtil.validateContactPattern(phoneNumber);
                            if(isValidPhoneNumber){
                                userDetails = new HashMap<>();
                                isEmailOrPhoneNumberValid = this.verifyUserRecordExists("phoneNumber", phoneNumber, userDetails);
                            }
                        }
                    }
                    if(!isEmailOrPhoneNumberValid){
                        errList.add("User Does not Exist, Invalid Email and Phone Number");
                    }
                    if (nextRow.getCell(2) != null && nextRow.getCell(2).getCellType() != CellType.BLANK) {
                        String dateOfJoining = nextRow.getCell(2).getStringCellValue().trim();
                        if(ValidationUtil.validateDate(dateOfJoining)){
                            valuesToBeUpdate.put(Constants.DATE_OF_JOINING, dateOfJoining);
                        }else{
                            errList.add("Invalid Date Of Joining");
                        }
                    }
                    if(!CollectionUtils.isEmpty(errList)){
                        this.setErrorDetails(str, errList, statusCell, errorDetails);
                        failedRecordsCount++;
                        totalRecordsCount++;
                        continue;
                    }
                    if (nextRow.getCell(3) != null && nextRow.getCell(3).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.DESIGNATION, nextRow.getCell(3).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(4) != null && nextRow.getCell(4).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.GROUP, nextRow.getCell(4).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(5) != null && nextRow.getCell(5).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.SERVICE, nextRow.getCell(5).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(6) != null && nextRow.getCell(6).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.CADRE, nextRow.getCell(6).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(7) != null && nextRow.getCell(7).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.PAY_TYPE, nextRow.getCell(7).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(8) != null && nextRow.getCell(8).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.INDUSTRY, nextRow.getCell(8).getStringCellValue().trim());
                    }
                    if (nextRow.getCell(9) != null && nextRow.getCell(9).getCellType() != CellType.BLANK) {
                        valuesToBeUpdate.put(Constants.LOCATION, nextRow.getCell(9).getStringCellValue().trim());
                    }
                    String userId = null;
                    if(!CollectionUtils.isEmpty(userDetails)){
                        userId = (String) userDetails.get(Constants.USER_ID);
                    }
                    List<WfStatusEntity> userPendingRequest = wfStatusRepo.findByUserIdAndCurrentStatus(userId, true);
                    boolean userRecordUpdate = true;
                    if(!CollectionUtils.isEmpty(userPendingRequest)){
                        for(WfStatusEntity wfStatusEntity : userPendingRequest){
                            String updateValuesString = wfStatusEntity.getUpdateFieldValues();
                            List<Map<String, Object>> updatedValues = mapper.readValue(updateValuesString, List.class);
                            Map<String, String> toValue = (Map<String, String>) updatedValues.get(0).get(Constants.TO_VALUE);
                            for(Map.Entry<String, String> entry : toValue.entrySet()){
                                if(valuesToBeUpdate.containsKey(entry.getKey())){
                                    WfRequest wfRequest = this.getWFRequest(wfStatusEntity, null);
                                    userProfileWfService.updateUserProfile(wfRequest);
                                    WfStatusEntity wfStatusEntityFailed = wfStatusRepo.findByWfId(wfRequest.getWfId());
                                    if(Constants.REJECTED.equalsIgnoreCase(wfStatusEntityFailed.getCurrentStatus())){
                                        userRecordUpdate = false;
                                        this.setErrorDetails(str, Collections.singletonList(Constants.UPDATE_FAILED), statusCell, errorDetails);
                                    } else{
                                        wfStatusEntity.setCurrentStatus(Constants.APPROVED);
                                        wfStatusEntity.setInWorkflow(false);
                                        wfStatusRepo.save(wfStatusEntity);
                                        statusCell.setCellValue(Constants.SUCCESS_UPPERCASE);
                                    }
                                    valuesToBeUpdate.remove(entry.getKey());
                                }
                            }
                        }
                    }
                    if(valuesToBeUpdate.isEmpty()){
                        if(userRecordUpdate)
                            noOfSuccessfulRecords++;
                        else
                            failedRecordsCount++;
                        totalRecordsCount++;
                        continue;
                    }
                    Set<String> employmentDetailsKey = new HashSet<>();
                    employmentDetailsKey.add(Constants.SERVICE);
                    employmentDetailsKey.add(Constants.CADRE);
                    employmentDetailsKey.add(Constants.PAY_TYPE);
                    WfRequest wfRequest = this.getWFRequest(valuesToBeUpdate, userId);
                    List<HashMap<String, Object>> updatedValues = new ArrayList<>();
                    for(Map.Entry<String, String> entry : valuesToBeUpdate.entrySet()){
                        String fieldKey;
                        HashMap<String, Object> updatedValueMap = new HashMap<>();
                        updatedValueMap.put(entry.getKey(), entry.getValue());
                        HashMap<String, Object> updateValues = new HashMap<>();
                        updateValues.put(Constants.FROM_VALUE, new HashMap<>());
                        updateValues.put(Constants.TO_VALUE, updatedValueMap);
                        if(employmentDetailsKey.contains(entry.getKey())){
                            fieldKey = Constants.EMPLOYMENT_DETAILS;
                        } else{
                            fieldKey = Constants.PROFESSIONAL_DETAILS;
                         }
                        updateValues.put(Constants.FIELD_KEY, fieldKey);
                        updatedValues.add(updateValues);
                        if(null != wfRequest){
                            wfRequest.setUpdateFieldValues(updatedValues);
                        }
                        userProfileWfService.updateUserProfileForBulkUpload(wfRequest);
                        WfStatusEntity wfStatusEntityFailed = wfStatusRepo.findByWfId(wfRequest.getWfId());
                        if(null != wfStatusEntityFailed && Constants.REJECTED.equalsIgnoreCase(wfStatusEntityFailed.getCurrentStatus())){
                            this.setErrorDetails(str, Collections.singletonList(Constants.UPDATE_FAILED), statusCell, errorDetails);
                            failedRecordsCount++;
                        } else{
                            if(userRecordUpdate){
                                noOfSuccessfulRecords++;
                                statusCell.setCellValue(Constants.SUCCESS_UPPERCASE);
                            } else{
                                this.setErrorDetails(str, Collections.singletonList(Constants.UPDATE_FAILED), statusCell, errorDetails);
                                failedRecordsCount++;
                            }
                        }
                    }
                    totalRecordsCount++;
                    duration = System.currentTimeMillis() - startTime;
                    logger.info("UserBulkUploadService:: Record Completed. Time taken: {} milli-seconds", duration);
                }
                if (totalRecordsCount == 0) {
                    XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                    Cell statusCell = row.createCell(10);
                    Cell errorDetails = row.createCell(11);
                    statusCell.setCellValue(Constants.FAILED_UPPERCASE);
                    errorDetails.setCellValue(Constants.EMPTY_FILE_FAILED);
                }
                status = uploadTheUpdatedFile(file, wb);
                if (!(Constants.SUCCESSFUL.equalsIgnoreCase(status) && failedRecordsCount == 0
                        && totalRecordsCount == noOfSuccessfulRecords && totalRecordsCount >= 1)) {
                    status = Constants.FAILED_UPPERCASE;
                }
            } else {
                logger.info("Error in Process Bulk Upload : The File is not downloaded/present");
                status = Constants.FAILED_UPPERCASE;
            }
            this.updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID), inputDataMap.get(Constants.IDENTIFIER),
                    status, totalRecordsCount, noOfSuccessfulRecords, failedRecordsCount);
        } catch (Exception e) {
            logger.error(String.format("Error in Process Bulk Upload %s", e.getMessage()), e);
            this.updateUserBulkUploadStatus(inputDataMap.get(Constants.ROOT_ORG_ID), inputDataMap.get(Constants.IDENTIFIER),
                    Constants.FAILED_UPPERCASE, 0, 0, 0);
        } finally {
            if (wb != null)
                wb.close();
            if (fis != null)
                fis.close();
            if (file != null)
                file.delete();
        }
    }

    private void setErrorDetails(StringBuffer str, List<String> errList, Cell statusCell, Cell errorDetails) {
        str.append("Failed to process user record. Missing Parameters - ").append(errList);
        statusCell.setCellValue(Constants.FAILED_UPPERCASE);
        errorDetails.setCellValue(str.toString());
    }

    private String uploadTheUpdatedFile(File file, XSSFWorkbook wb)
            throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        wb.write(fileOut);
        fileOut.close();
        SBApiResponse uploadResponse = storageService.uploadFile(file, configuration.getUserBulkUpdateFolderName(), configuration.getWorkflowCloudContainerName());
        if (!HttpStatus.OK.equals(uploadResponse.getResponseCode())) {
            String errMsg = String.format("Failed to upload file. Error: %s", uploadResponse.getResult().get(Constants.ERROR_MESSAGE));
            logger.info(errMsg);
            return "FAILED";
        }
        return "SUCCESSFUL";
    }


    public boolean verifyUserRecordExists(String field, String fieldValue, Map<String, Object> userRecordDetails) {

        HashMap<String, String> headersValue = new HashMap<>();
        headersValue.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);

        Map<String, Object> filters = new HashMap<>();
        filters.put(field, fieldValue);

        Map<String, Object> request = new HashMap<>();
        request.put("filters", filters);
        request.put(Constants.FIELDS, Arrays.asList(Constants.USER_ID, Constants.STATUS, Constants.CHANNEL, Constants.ROOT_ORG_ID, Constants.PHONE, Constants.EMAIL));

        Map<String, Object> requestObject = new HashMap<>();
        requestObject.put("request", request);
        try {
            StringBuilder builder = new StringBuilder(configuration.getLmsServiceHost());
            builder.append(configuration.getLmsUserSearchEndPoint());
            Map<String, Object> userSearchResult = (Map<String, Object>) requestServiceImpl
                    .fetchResultUsingPost(builder, requestObject, Map.class, headersValue);
            if (userSearchResult != null
                    && "OK".equalsIgnoreCase((String) userSearchResult.get(Constants.RESPONSE_CODE))) {
                Map<String, Object> map = (Map<String, Object>) userSearchResult.get(Constants.RESULT);
                Map<String, Object> response = (Map<String, Object>) map.get(Constants.RESPONSE);
                List<Map<String, Object>> contents = (List<Map<String, Object>>) response.get(Constants.CONTENT);
                if (!CollectionUtils.isEmpty(contents)) {
                    for (Map<String, Object> content : contents) {
                        userRecordDetails.put(Constants.USER_ID, content.get(Constants.USER_ID));
                        userRecordDetails.put(Constants.DEPARTMENT_NAME, content.get(Constants.CHANNEL));
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching user details : ",e);
            throw new ApplicationException("Hub Service ERROR: ", e);
        }
        return false;
    }

    private WfRequest getWFRequest(Object object, String userId) throws IOException {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setAction(Constants.APPROVE_STATE);
        String wfId = UUID.randomUUID().toString();
        if(object instanceof HashMap){
            Map<String, String> userDetails = (Map<String, String>) object;
            wfRequest.setApplicationId(userId);
            wfRequest.setUserId(userDetails.get(Constants.USER_ID));
            wfRequest.setAction(Constants.APPROVE_STATE);
            wfRequest.setRootOrgId("");
            wfRequest.setDeptName("");
            wfRequest.setState(Constants.SEND_FOR_APPROVAL);
            wfRequest.setServiceName(Constants.PROFILE_SERVICE_NAME);
            wfRequest.setActorUserId("");
            wfRequest.setComment("Bulk Update by MDO Admin");
            wfRequest.setWfId(wfId);
            return wfRequest;
        }
        if(object instanceof WfStatusEntity){
            WfStatusEntity wfStatusEntity = (WfStatusEntity) object;
            wfRequest.setWfId(wfStatusEntity.getWfId());
            wfRequest.setComment(wfStatusEntity.getComment());
            wfRequest.setDeptName(wfStatusEntity.getDeptName());
            wfRequest.setRootOrgId(wfStatusEntity.getRootOrg());
            wfRequest.setState(wfStatusEntity.getCurrentStatus());
            wfRequest.setActorUserId(wfStatusEntity.getActorUUID());
            wfRequest.setServiceName(wfStatusEntity.getServiceName());
            wfRequest.setApplicationId(wfStatusEntity.getApplicationId());
            wfRequest.setUpdateFieldValues(mapper.readValue(wfStatusEntity.getUpdateFieldValues(), List.class));
            return wfRequest;
        }
        return null;
    }

}