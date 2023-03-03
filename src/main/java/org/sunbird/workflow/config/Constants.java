package org.sunbird.workflow.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {

	private Constants() {
	}

	public static final String ROOT_ORG = "igot";

	public static final String ORG = "dopt";

	public static final String MESSAGE = "message";

	public static final String DATA = "data";

	public static final String STATUS = "status";

	public static final String WF_ID_CONSTANT = "wfId";

	public static final String WF_IDS_CONSTANT = "wfIds";

	public static final String SUCCESSFUL = "Successful";

	public static final String STATUS_CHANGE_MESSAGE = "Application status changed to ";

	public static final String WORKFLOW_PARSING_ERROR_MESSAGE = "Workflow parsing error occurred!";

	public static final String WORKFLOW_INITIATE_ERROR_MESSAGE = "Workflow is not initiated!";

	public static final String WORKFLOW_ROLE_ERROR = "No role found for the actor!";

	public static final String WORKFLOW_ROLE_CHECK_ERROR = "No proper role found for the actor!";

	public static final String WORKFLOW_ACTION_ERROR = "No action found on given action!";

	public static final String WORKFLOW_STATE_CHECK_ERROR = "No wf state found on given state!";

	public static final String STATE_VALIDATION_ERROR = "Work flow state can not be empty!";
	public static final String STATUS_VALIDATION_ERROR_FOR_INITIATE = "Work flow state must be initiate!";
	public static final String STATUS_VALIDATION_ERROR_FOR_NOT_INITIATE = "Work flow state must not be initiate!";

	public static final String ACTOR_UUID_VALIDATION_ERROR = "actor uuid can not be empty!";

	public static final String APPLICATION_ID_VALIDATION_ERROR = "application id can not be empty!";

	public static final String USER_UUID_VALIDATION_ERROR = "user uuid can not be empty!";

	public static final String ACTION_VALIDATION_ERROR = "Work flow action can not be empty!";

	public static final String FIELD_VALUE_VALIDATION_ERROR = "Update Field values can not be empty!";

	public static final String WORKFLOW_SERVICENAME_VALIDATION_ERROR = "Workflow service name can not be empty!";

	public static final String JSON_PARSING_ERROR = "Json parsing error occured!";

	public static final String WORKFLOW_ID_ERROR_MESSAGE = "Workflow id can not be empty!";

	public static final String ACTION_CONSTANT = "action";

	public static final String ROLES_CONSTANT = "roles";

	public static final String IS_WORKFLOW_TERMINATED = "inWorkflow";

	public static final String SEARCH_CRITERIA_VALIDATION = "Provide the mandatory fields for search!";

	public static final String PROFILE_SERVICE_NAME = "profile";

	public static final String SERVICE_NAME_EXCEPTION = "No configured service found!";

	public static final String USER_ID_VALUE = "userIdValue";

	public static final String APPROVED_STATE = "APPROVED";

	public static final String PROCESSED_STATE = "PROCESSED";

	public static final String EMAIL = "email";

	public static final String LAST_NAME = "last_name";

	public static final String FIRST_NAME = "first_name";

	public static final String UUID = "wid";

	public static final List<String> USER_DEFAULT_FIELDS = Collections
			.unmodifiableList(Arrays.asList(UUID, FIRST_NAME, LAST_NAME, EMAIL));

	public static final String CBP_WF_SERVICE_NAME = "cbp-content";

	public static final String USER_PROFILE_FLAG_SERVICE = "userprofile_flag_service";

	public static final String APPROVE_STATE = "APPROVE";

	public static final String ROOT_ORG_CONSTANT = "rootOrg";

	public static final String ROOT_ORG_ID = "rootOrgId";

	public static final String USER_ID = "userId";

	public static final String FIRSTNAME = "firstname";

	public static final String LASTNAME = "surname";

	public static final String PRIMARY_EMAIL = "primaryEmail";

	public static final String RESULT = "result";
	public static final String RESPONSE_CODE = "responseCode";
	public static final String RESPONSE = "response";
	public static final String CONTENT = "content";
	public static final String PROFILE_DETAILS = "profileDetails";
	public static final String REQUEST = "request";
	public static final String PERSONAL_DETAILS = "personalDetails";
	public static final String EMPLOYMENT_DETAILS = "employmentDetails";
	public static final String DEPARTMENT_NAME = "departmentName";
	public static final String USER_REGISTRATION_SERVICE_NAME = "user_registration";
	public static final String WF_APPROVED_STATE = "WF_APPROVED";
	public static final String OSID = "osid";
	public static final String FIELD_KEY = "fieldKey";
	public static final String TO_VALUE = "toValue";
	public static final String FROM_VALUE = "fromValue";
	public static final String PROFESSIONAL_DETAILS = "professionalDetails";
	public static final String CHANNEL = "channel";
	public static final String FORCE_MIGRATION = "forceMigration";
	public static final String SOFT_DELETE_OLD_GROUP = "softDeleteOldOrg";
	public static final String NOTIFY_MIGRATION = "notifyMigration";
	public static final String X_AUTH_USER_ID = "x-authenticated-userid";
	public static final String ORG_NAME = "orgName";
	public static final String OK = "OK";
	public static final String FAILED = "FAILED";
	public static final String PARAMS = "params";
	public static final String ERROR_MESSAGE = "errmsg";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String APPLICATION_JSON = "application/json";
	public static final String REJECTED = "REJECTED";
	public static final String HTTP_STATUS_CODE = "httpStatusCode";
	public static final String NAME = "name";
	public static final String ORGANIZATION_ID = "organisationId";
	public static final String ROLES = "roles";
	public static final String PUBLIC = "PUBLIC";
	public static final String VALUE = "value";

	public static final String TAXONOMY_SERVICE_NAME = "taxonomy";
	public static final String INITIATE = "initiate";

	public static final String DOT_SEPARATOR = ".";
	public static final String UNAUTHORIZED_KEY = "Unauthorized";
	public static final String SHA_256_WITH_RSA = "SHA256withRSA";
	public static final String SUB = "sub";
	public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken_publickey_basepath";


}
