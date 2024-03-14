package org.sunbird.workflow.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.service.StorageService;
import scala.Option;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class StorageServiceImpl implements StorageService {

    private Logger logger = LoggerFactory.getLogger(getClass().getName());
    private BaseStorageService storageService = null;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private Configuration configuration;

//    @Autowired
//    private AccessTokenValidator accessTokenValidator;

    @PostConstruct
    public void init() {
        if (storageService == null) {
            storageService = StorageServiceFactory.getStorageService(new StorageConfig(
                    configuration.getCloudStorageTypeName(), configuration.getCloudStorageKey(),
                    configuration.getCloudStorageSecret(), Option.apply(configuration.getCloudStorageCephs3Endpoint())));
        }
    }

    public Response uploadFile(MultipartFile mFile, String cloudFolderName, String containerName) throws IOException {
        Response response = new Response();
        File file = null;
        try {
            file = new File(System.currentTimeMillis() + "_" + mFile.getOriginalFilename());
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(mFile.getBytes());
            fos.close();
            return uploadFile(file, cloudFolderName,containerName);
        } catch (Exception e) {
            logger.error("Failed to Upload File Exception", e);

            return response;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }


    @Override
    public Response downloadFile(String fileName) {
        Response response = new Response();
        try {
            String objectKey = configuration.getBulkUploadContainerName() + "/" + fileName;
            storageService.download(configuration.getCloudContainerName(), objectKey, Constants.LOCAL_BASE_PATH,
                    Option.apply(Boolean.FALSE));
            return response;
        } catch (Exception e) {
            logger.error("Failed to Download File" + fileName + ", Exception : ", e);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response;
        }
    }

    public Response uploadFile(File file, String cloudFolderName, String containerName) {
        Response response = new Response();
        try {
            String objectKey = cloudFolderName + "/" + file.getName();
            String url = storageService.upload(containerName, file.getAbsolutePath(),
                    objectKey, Option.apply(false), Option.apply(1), Option.apply(5), Option.empty());
            Map<String, String> uploadedFile = new HashMap<>();
            uploadedFile.put("name", file.getName());
            uploadedFile.put("url", url);
            response.getResult().putAll(uploadedFile);
            return response;
        } catch (Exception e) {
            logger.error("Failed tp upload file", e);
            response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
            response.put(Constants.ERROR_MESSAGE, e.getMessage());
            return response;
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    protected void finalize() {
        try {
            if (storageService != null) {
                storageService.closeContext();
                storageService = null;
            }
        } catch (Exception e) {
        }
    }

}
