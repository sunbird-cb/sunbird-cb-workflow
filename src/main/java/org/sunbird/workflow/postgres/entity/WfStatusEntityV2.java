package org.sunbird.workflow.postgres.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "wf_statusV2", schema = "wingspan")
public class WfStatusEntityV2 {

	@Id
	@Column(name = "wf_id", nullable = false)
	private String wfId;

	@Column(name = "userid", nullable = false)
	private String userId;

	@Column(name = "current_status")
	private String currentStatus;

	@Column(name = "application_id")
	private String applicationId;

	@Column(name = "in_workflow")
	private boolean inWorkflow;

	@Column(name = "service_name")
	private String serviceName;

	@Column(name = "created_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdOn;

	@Column(name = "lastupdated_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastUpdatedOn;

	@Column(name = "update_field_values")
	private String updateFieldValues;

	@Column(name = "dept_name")
	private String deptName;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Date getLastUpdatedOn() {
		return lastUpdatedOn;
	}

	public void setLastUpdatedOn(Date lastUpdatedOn) {
		this.lastUpdatedOn = lastUpdatedOn;
	}

	public String getWfId() {
		return wfId;
	}

	public void setWfId(String wfId) {
		this.wfId = wfId;
	}

	public boolean getInWorkflow() {
		return inWorkflow;
	}

	public void setInWorkflow(boolean inWorkflow) {
		this.inWorkflow = inWorkflow;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getUpdateFieldValues() {
		return updateFieldValues;
	}

	public void setUpdateFieldValues(String updateFieldValues) {
		this.updateFieldValues = updateFieldValues;
	}

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
}
