package org.sunbird.workflow.utils;

import java.util.List;
import java.util.Map;

public interface CassandraOperation {

	List<Map<String, Object>> getRecordsByProperties(String keyspaceName, String tableName,
			Map<String, Object> propertyMap, List<String> fields);

	int getCountByProperties(String keyspaceName, String tableName, Map<String, Object> propertyMap);
}
