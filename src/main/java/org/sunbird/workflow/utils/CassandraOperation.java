package org.sunbird.workflow.utils;

import org.sunbird.workflow.models.Response;

import java.util.List;
import java.util.Map;

public interface CassandraOperation {

	public Response insertRecord(String keyspaceName, String tableName, Map<String, Object> request);

	List<Map<String, Object>> getRecordsByProperties(String keyspaceName, String tableName,
			Map<String, Object> propertyMap, List<String> fields);

}
