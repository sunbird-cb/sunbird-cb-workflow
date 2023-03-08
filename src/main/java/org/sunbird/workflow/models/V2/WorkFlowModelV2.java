package org.sunbird.workflow.models.V2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkFlowModelV2 {

    private List<WfStatusV2> wfstates;

    public List<WfStatusV2> getWfstates() {
        return wfstates;
    }

    public void setWfstates(List<WfStatusV2> wfstates) {
        this.wfstates = wfstates;
    }
}
