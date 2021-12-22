package org.sunbird.workflow.models.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("system_settings")
public class Workflow {

    @PrimaryKey
    private WfPrimaryKey wfPrimarykey;

    @Column("value")
    private String configuration;

    public WfPrimaryKey getProfileWfPrimarykey() {
        return wfPrimarykey;
    }

    public void setProfileWfPrimarykey(WfPrimaryKey wfPrimarykey) {
        this.wfPrimarykey = wfPrimarykey;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Workflow() {
        super();
    }
    
    public Workflow(WfPrimaryKey wfPrimarykey, String configuration) {
        super();
        this.wfPrimarykey = wfPrimarykey;
        this.configuration = configuration;
    }
}
