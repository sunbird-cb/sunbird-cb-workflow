package org.sunbird.workflow.cassandra.entity;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.util.Date;

@Table("email_template")
public class EmailTemplateEntity {

    @PrimaryKey
    private String name;

    @Column("createdby")
    private String createdBy;

    @Column("createdon")
    private Date createdOn;

    @Column("lastupdatedby")
    private String lastUpdatedBy;

    @Column("lastupdatedon")
    private Date lastUpdatedOn;

    @Column("template")
    private String template;

    public EmailTemplateEntity() {
        super();
    }

    public EmailTemplateEntity(String name, String createdBy, Date createdOn, String lastUpdatedBy, Date lastUpdatedOn, String template) {
        super();
        this.name = name;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdatedOn = lastUpdatedOn;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Date getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public void setLastUpdatedOn(Date lastUpdatedOn) {
        this.lastUpdatedOn = lastUpdatedOn;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
