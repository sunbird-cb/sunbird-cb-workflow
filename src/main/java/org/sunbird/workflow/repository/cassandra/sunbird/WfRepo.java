package org.sunbird.workflow.repository.cassandra.sunbird;


import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;
import org.sunbird.workflow.models.cassandra.WfPrimaryKey;
import org.sunbird.workflow.models.cassandra.Workflow;

@Repository
public interface WfRepo extends CassandraRepository<Workflow, WfPrimaryKey> {

    @Query("SELECT * FROM system_settings WHERE id=?0;")
    Workflow getWorkFlowForService(String id);

}
