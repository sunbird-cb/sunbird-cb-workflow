package org.sunbird.workflow.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestTerm {

    private Map<String, Object> term = new HashMap<String, Object>();

    public Map<String, Object> getTerm() {
        return term;
    }

    public void setTerm(Map<String, Object> term) {
        this.term = term;
    }

}
