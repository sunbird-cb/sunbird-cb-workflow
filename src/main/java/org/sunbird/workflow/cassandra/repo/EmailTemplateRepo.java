package org.sunbird.workflow.cassandra.repo;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.sunbird.workflow.cassandra.entity.EmailTemplateEntity;

@Repository
public interface EmailTemplateRepo extends CassandraRepository<EmailTemplateEntity, String> {
    EmailTemplateEntity findByName(String name);
}
