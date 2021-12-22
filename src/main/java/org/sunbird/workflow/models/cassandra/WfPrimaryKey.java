package org.sunbird.workflow.models.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
public class WfPrimaryKey implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.PARTITIONED)
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WfPrimaryKey() {
        super();
    }

    public WfPrimaryKey(String id) {
        super();
        this.id = id;
    }
}
