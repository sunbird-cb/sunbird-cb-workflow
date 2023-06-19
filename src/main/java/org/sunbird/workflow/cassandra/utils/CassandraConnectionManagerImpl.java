package org.sunbird.workflow.cassandra.utils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CassandraConnectionManagerImpl implements CassandraConnectionManager{

	private static Map<String, Session> cassandraSessionMap = new ConcurrentHashMap<>(2);
	
	@Autowired
	private Cluster cluster;
	  
	@Override
	public Session getSession(String keyspaceName) {
		Session session = cassandraSessionMap.get(keyspaceName);
	    if (null != session) {
	      return session;
	    } else {
	      Session session2 = cluster.connect(keyspaceName);
	      cassandraSessionMap.put(keyspaceName, session2);
	      return session2;
	    }
	}

}
