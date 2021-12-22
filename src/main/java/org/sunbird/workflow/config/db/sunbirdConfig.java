package org.sunbird.workflow.config.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cassandra.config.CassandraClusterFactoryBean;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.PlainTextAuthProvider;

@Configuration
@ConfigurationProperties("spring.data.cassandra.sunbird")
@EnableCassandraRepositories(basePackages = "org.sunbird.workflow.repository.cassandra.sunbird", cassandraTemplateRef = "sunbirdTemplate")
public class sunbirdConfig extends CassandraConfig {

	private Logger logger = LoggerFactory.getLogger(sunbirdConfig.class);

	@Value("${spring.data.cassandra.sunbird.username}")
	private String user;

	@Value("${spring.data.cassandra.sunbird.password}")
	private String password;

	@Override
	@Primary
	@Bean(name = "sunbirdTemplate")
	public CassandraAdminTemplate cassandraTemplate() throws Exception {
		return new CassandraAdminTemplate(session().getObject(), cassandraConverter());
	}

	@Override
	@Bean(name = "sunbirdSession")
	public CassandraSessionFactoryBean session() {

		AuthProvider authProvider = new PlainTextAuthProvider(user, password);

		CassandraClusterFactoryBean cluster = new CassandraClusterFactoryBean();
		cluster.setContactPoints(getContactPoints());
		cluster.setPort(getPort());
		cluster.setAuthProvider(authProvider);
		cluster.setJmxReportingEnabled(false);
		try {
			cluster.afterPropertiesSet();
		} catch (Exception e) {
			logger.error("Failed to construct Cassandra Cluster Object. ", e);
			return null;
		}
		
		CassandraSessionFactoryBean session = new CassandraSessionFactoryBean();
		session.setCluster(cluster.getObject());
		session.setConverter(cassandraConverter());
		session.setKeyspaceName(getKeyspaceName());
		session.setSchemaAction(getSchemaAction());
		session.setStartupScripts(getStartupScripts());
		session.setShutdownScripts(getShutdownScripts());
		logger.info("Cassandra session created for " + getKeyspaceName() + "keyspace with IP : " + getContactPoints()
				+ ":" + getPort());
		return session;
	}
}
