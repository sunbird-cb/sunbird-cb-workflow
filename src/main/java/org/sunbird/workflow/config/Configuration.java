package org.sunbird.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Configuration {

    @Value("${workflow.pagination.default.limit}")
    private Integer defaultLimit;

    @Value("${workflow.pagination.default.offset}")
    private Integer defaultOffset;

    @Value("${workflow.pagination.max.limit}")
    private Integer maxLimit;

    @Value("${hub.service.host}")
    private String hubServiceHost;

    @Value("${hub.profile.update}")
    private String hubProfileUpdateEndPoint;

    @Value("${lms.user.read}")
    private String userProfileReadEndPoint;

    @Value("${lms.user.update}")
    private String userProfileUpdateEndPoint;

    @Value("${lms.user.migrate}")
    private String userProfileMigrateEndPoint;

    @Value("${lms.service.host}")
    private String lmsServiceHost;

    @Value("${lms.user.search}")
    private String lmsUserSearchEndPoint;

    @Value("${lms.assign.role}")
    private String lmsAssignRoleEndPoint;

    @Value("${pid.service.host}")
    private String pidServiceHost;

    @Value("${pid.multiplesearch.endpoint}")
    private String multipleSearchEndPoint;

    @Value("${lexcore.service.host}")
    private String lexCoreServiceHost;

    @Value("${userrole.search.endpoint}")
    private String userRoleSearchEndpoint;

    @Value("${kafka.topics.workflow.request}")
    private String workflowApplicationTopic;

    @Value("${multiple-workflow-creation}")
    private boolean multipleWfCreationEnable;

    @Value("${notify.service.host}")
    private String notifyServiceHost;

    @Value("${notify.service.path}")
    private String notifyServicePath;

    @Value("${hub.notification.rootOrg}")
    private String hubRootOrg;

    @Value("${portal.departmentupdate.path}")
    private String departmentUpdatePath;

    @Value("${hub.profile.search}")
    private String hubProfileSearchEndPoint;

    @Value("${kafka.topics.workflow.notification}")
    private String workFlowNotificationTopic;
    
    @Value("${kafka.topics.user.registration.createUser}")
    private String workflowCreateUserTopic;

    @Value("${lms.system.settings.wfProfileService.path}")
    private String profileServiceConfigPath;

    @Value("${sso.url}")
    private String ssoUrl;

    @Value("${sso.realm}")
    private String ssoRealm;

    @Value("{accesstoken.publickey.basepath}")
    private String accessTokenKey;

    public Integer getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(Integer defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public Integer getDefaultOffset() {
        return defaultOffset;
    }

    public void setDefaultOffset(Integer defaultOffset) {
        this.defaultOffset = defaultOffset;
    }

    public Integer getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(Integer maxLimit) {
        this.maxLimit = maxLimit;
    }

    public String getHubServiceHost() {
        return hubServiceHost;
    }

    public void setHubServiceHost(String hubServiceHost) {
        this.hubServiceHost = hubServiceHost;
    }

    public String getUserProfileReadEndPoint() {
        return userProfileReadEndPoint;
    }

    public void setUserProfileReadEndPoint(String userProfileReadEndPoint) {
        this.userProfileReadEndPoint = userProfileReadEndPoint;
    }

    public String getUserProfileUpdateEndPoint() {
        return userProfileUpdateEndPoint;
    }

    public void setUserProfileUpdateEndPoint(String userProfileUpdateEndPoint) {
        this.userProfileUpdateEndPoint = userProfileUpdateEndPoint;
    }

    public String getUserProfileMigrateEndPoint() {
        return userProfileMigrateEndPoint;
    }

    public void setUserProfileMigrateEndPoint(String userProfileMigrateEndPoint) {
        this.userProfileMigrateEndPoint = userProfileMigrateEndPoint;
    }

    public String getLmsAssignRoleEndPoint() {
        return lmsAssignRoleEndPoint;
    }

    public void setLmsAssignRoleEndPoint(String lmsAssignRoleEndPoint) {
        this.lmsAssignRoleEndPoint = lmsAssignRoleEndPoint;
    }

    public String getHubProfileUpdateEndPoint() {
        return hubProfileUpdateEndPoint;
    }

    public void setHubProfileUpdateEndPoint(String hubProfileUpdateEndPoint) {
        this.hubProfileUpdateEndPoint = hubProfileUpdateEndPoint;
    }

    public String getPidServiceHost() {
        return pidServiceHost;
    }

    public void setPidServiceHost(String pidServiceHost) {
        this.pidServiceHost = pidServiceHost;
    }

    public String getMultipleSearchEndPoint() {
        return multipleSearchEndPoint;
    }

    public void setMultipleSearchEndPoint(String multipleSearchEndPoint) {
        this.multipleSearchEndPoint = multipleSearchEndPoint;
    }

    public String getLexCoreServiceHost() {
        return lexCoreServiceHost;
    }

    public void setLexCoreServiceHost(String lexCoreServiceHost) {
        this.lexCoreServiceHost = lexCoreServiceHost;
    }

    public String getUserRoleSearchEndpoint() {
        return userRoleSearchEndpoint;
    }

    public void setUserRoleSearchEndpoint(String userRoleSearchEndpoint) {
        this.userRoleSearchEndpoint = userRoleSearchEndpoint;
    }

    public String getWorkflowApplicationTopic() {
        return workflowApplicationTopic;
    }

    public void setWorkflowApplicationTopic(String workflowApplicationTopic) {
        this.workflowApplicationTopic = workflowApplicationTopic;
    }

    public boolean getMultipleWfCreationEnable() {
        return multipleWfCreationEnable;
    }

    public void setMultipleWfCreationEnable(boolean multipleWfCreationEnable) {
        this.multipleWfCreationEnable = multipleWfCreationEnable;
    }

    public boolean isMultipleWfCreationEnable() {
        return multipleWfCreationEnable;
    }

    public String getNotifyServiceHost() {
        return notifyServiceHost;
    }

    public void setNotifyServiceHost(String notifyServiceHost) {
        this.notifyServiceHost = notifyServiceHost;
    }

    public String getNotifyServicePath() {
        return notifyServicePath;
    }

    public void setNotifyServicePath(String notifyServicePath) {
        this.notifyServicePath = notifyServicePath;
    }

    public String getHubRootOrg() {
        return hubRootOrg;
    }

    public void setHubRootOrg(String hubRootOrg) {
        this.hubRootOrg = hubRootOrg;
    }

    public String getDepartmentUpdatePath() {
        return departmentUpdatePath;
    }

    public void setDepartmentUpdatePath(String departmentUpdatePath) {
        this.departmentUpdatePath = departmentUpdatePath;
    }

    public String getHubProfileSearchEndPoint() {
        return hubProfileSearchEndPoint;
    }

    public void setHubProfileSearchEndPoint(String hubProfileSearchEndPoint) {
        this.hubProfileSearchEndPoint = hubProfileSearchEndPoint;
    }

    public String getWorkFlowNotificationTopic() {
        return workFlowNotificationTopic;
    }

    public void setWorkFlowNotificationTopic(String workFlowNotificationTopic) {
        this.workFlowNotificationTopic = workFlowNotificationTopic;
    }
    public String getLmsServiceHost() { return lmsServiceHost; }

    public void setLmsServiceHost(String lmsServiceHost) {
        this.lmsServiceHost = lmsServiceHost;
    }

    public String getLmsUserSearchEndPoint() { return lmsUserSearchEndPoint; }

    public void setLmsUserSearchEndPoint(String lmsUserSearchEndPoint) {
        this.lmsUserSearchEndPoint = lmsUserSearchEndPoint;
    }

	public String getWorkflowCreateUserTopic() {
		return workflowCreateUserTopic;
	}

	public void setWorkflowCreateUserTopic(String workflowCreateUserTopic) {
		this.workflowCreateUserTopic = workflowCreateUserTopic;
	}

    public String getProfileServiceConfigPath() {
        return profileServiceConfigPath;
    }

    public void setProfileServiceConfigPath(String profileServiceConfigPath) {
        this.profileServiceConfigPath = profileServiceConfigPath;
    }

    public String getSsoUrl() {
        return ssoUrl;
    }

    public void setSsoUrl(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }

    public String getSsoRealm() {
        return ssoRealm;
    }

    public void setSsoRealm(String ssoRealm) {
        this.ssoRealm = ssoRealm;
    }

    public String getAccessTokenKey() {
        return accessTokenKey;
    }

    public void setAccessTokenKey(String accessTokenKey) {
        this.accessTokenKey = accessTokenKey;
    }
}
