package org.sunbird.workflow.models.V2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sunbird.workflow.models.WfAction;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WfStatusV2 {

    private String state;

    private String status; //need to verify

    @JsonProperty(value = "isStartState")
    private Boolean isStartState;

    private Boolean isLastState;

    @JsonProperty(value = "isNotificationEnable")
    private Boolean isNotificationEnable;

    private List<WfAction> actions;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Boolean getIsLastState() {
        return isLastState;
    }

    public void setIsLastState(Boolean isLastState) {
        this.isLastState = isLastState;
    }

    public List<WfAction> getActions() {
        return actions;
    }

    public void setActions(List<WfAction> actions) {
        this.actions = actions;
    }

    public Boolean getStartState() {
        return isStartState;
    }

    public void setStartState(Boolean startState) {
        isStartState = startState;
    }

    public Boolean getNotificationEnable() {
        return isNotificationEnable;
    }

    public void setNotificationEnable(Boolean notificationEnable) {
        isNotificationEnable = notificationEnable;
    }
}
