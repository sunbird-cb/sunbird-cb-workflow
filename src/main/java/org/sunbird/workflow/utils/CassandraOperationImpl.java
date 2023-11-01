package org.sunbird.workflow.utils;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.*;
import com.datastax.driver.core.querybuilder.Select.Builder;
import com.datastax.driver.core.querybuilder.Select.Where;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.sunbird.workflow.config.Constants;

import java.util.*;
import java.util.Map.Entry;

@Component
public class CassandraOperationImpl implements CassandraOperation {

	private Logger logger = LoggerFactory.getLogger(getClass().getName());
	protected CassandraConnectionManager connectionManager = CassandraConnectionMngrFactory.getInstance();

	@Override
	public List<Map<String, Object>> getRecordsByProperties(String keyspaceName, String tableName,
			Map<String, Object> propertyMap, List<String> fields) {
		Select selectQuery = null;
		List<Map<String, Object>> response = new ArrayList<>();
		try {
			selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);
			ResultSet results = connectionManager.getSession(keyspaceName).execute(selectQuery);
			response = CassandraUtil.createResponse(results);

		} catch (Exception e) {
			logger.error(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
		}
		return response;
	}

	@Override
	public int getCountByProperties(String keyspaceName, String tableName, Map<String, Object> propertyMap) {
		Select.Where selectQuery = QueryBuilder.select().countAll().from(keyspaceName, tableName).where();
		for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
			selectQuery.and(QueryBuilder.eq(entry.getKey(), entry.getValue()));
		}
		int count = 0;
		try {
			ResultSet resultSet = connectionManager.getSession(keyspaceName).execute(selectQuery);
			Row row = resultSet.one();
			if (row != null) {
				count = (int) row.getLong(0);
			}
		} catch (Exception e) {
			logger.error(Constants.EXCEPTION_MSG_FETCH + tableName + " : " + e.getMessage(), e);
		}
		return count;
	}

	private Select processQuery(String keyspaceName, String tableName, Map<String, Object> propertyMap,
			List<String> fields) {
		Select selectQuery = null;

		Builder selectBuilder;
		if (CollectionUtils.isNotEmpty(fields)) {
			String[] dbFields = fields.toArray(new String[fields.size()]);
			selectBuilder = QueryBuilder.select(dbFields);
		} else {
			selectBuilder = QueryBuilder.select().all();
		}
		selectQuery = selectBuilder.from(keyspaceName, tableName);
		if (MapUtils.isNotEmpty(propertyMap)) {
			Where selectWhere = selectQuery.where();
			for (Entry<String, Object> entry : propertyMap.entrySet()) {
				if (entry.getValue() instanceof List) {
					List<Object> list = (List) entry.getValue();
					if (null != list) {
						Object[] propertyValues = list.toArray(new Object[list.size()]);
						Clause clause = QueryBuilder.in(entry.getKey(), propertyValues);
						selectWhere.and(clause);

					}
				} else {

					Clause clause = QueryBuilder.eq(entry.getKey(), entry.getValue());
					selectWhere.and(clause);

				}
				selectQuery.allowFiltering();
			}
		}
		return selectQuery;
	}

	public void insertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
		String query = CassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
		try {
			PreparedStatement statement = connectionManager.getSession(keyspaceName).prepare(query);
			BoundStatement boundStatement = new BoundStatement(statement);
			Iterator<Object> iterator = request.values().iterator();
			Object[] array = new Object[request.keySet().size()];
			int i = 0;
			while (iterator.hasNext()) {
				array[i++] = iterator.next();
			}
			connectionManager.getSession(keyspaceName).execute(boundStatement.bind(array));
		} catch (Exception e) {
			String.format("Exception occurred while inserting record to %s %s", tableName, e.getMessage());
		}
	}
}