package org.sunbird.workflow.models;

import java.util.Date;

public class WfNotification extends WfRequest{

    private String courseName;

    private String batchName;

    private Date batchStartDate;

    private boolean isNominatedByMdo;

    public boolean isNominatedByMdo() { return isNominatedByMdo; }

    public void setNominatedByMdo(boolean nominatedByMdo) { isNominatedByMdo = nominatedByMdo; }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public Date getBatchStartDate() {
        return batchStartDate;
    }

    public void setBatchStartDate(Date batchStartDate) {
        this.batchStartDate = batchStartDate;
    }
}