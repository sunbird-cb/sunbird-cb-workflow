package org.sunbird.workflow.service;


import org.springframework.web.multipart.MultipartFile;
import org.sunbird.workflow.models.Response;

import java.io.File;
import java.io.IOException;

public interface StorageService {

	public Response downloadFile(String fileName);

	public Response uploadFile(File file, String cloudFolderName, String containerName);

	public Response uploadFile(MultipartFile file, String cloudFolderName, String containerName) throws IOException;

}
