package org.sunbird.workflow.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {

    private RequestTerm request;

    public RequestTerm getRequest() {
        return request;
    }

    public void setRequest(RequestTerm request) {
        this.request = request;
    }
}
