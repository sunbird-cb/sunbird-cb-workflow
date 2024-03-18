package org.sunbird.workflow.service;


import java.io.File;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import org.sunbird.workflow.models.SBApiResponse;

public interface StorageService {

	public SBApiResponse downloadFile(String fileName);

	public SBApiResponse uploadFile(File file, String cloudFolderName, String containerName);

	public SBApiResponse uploadFile(MultipartFile file, String cloudFolderName, String containerName) throws IOException;

}
