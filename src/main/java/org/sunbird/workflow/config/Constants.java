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

	public static final String BLENDED_PROGRAM_SERVICE_NAME = "blendedprogram";
	public static final String POSITION_SERVICE_NAME = "position";
	public static final String ORGANISATION_SERVICE_NAME = "organisation";
	public static final String DOMAIN_SERVICE_NAME = "domain";

	public static final String SERVICE_NAME_EXCEPTION = "No configured service found!";

	public static final String USER_ID_VALUE = "userIdValue";

	public static final String APPROVED_STATE = "APPROVED";

	public static final String PROCESSED_STATE = "PROCESSED";

	public static final String EMAIL = "email";

	public static final String FIRST_NAME = "first_name";

	public static final String UUID = "wid";

	public static final List<String> USER_DEFAULT_FIELDS = Collections
			.unmodifiableList(Arrays.asList(UUID, FIRST_NAME, EMAIL));

	public static final String CBP_WF_SERVICE_NAME = "cbp-content";

	public static final String USER_PROFILE_FLAG_SERVICE = "userprofile_flag_service";

	public static final String APPROVE_STATE = "APPROVE";

	public static final String ROOT_ORG_CONSTANT = "rootOrg";

	public static final String ROOT_ORG_ID = "rootOrgId";

	public static final String USER_ID = "userId";

	public static final String FIRSTNAME = "firstname";

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
	public static final boolean POSITION_SEARCH_ENABLED = true;
	public static final boolean DOMAIN_SEARCH_ENABLED = true;
	public static final boolean ORG_SEARCH_ENABLED = true;

	public static final boolean BLENDED_PROGRAM_SEARCH_ENABLED = true;
	public static final String MDO_ADMIN = "MDO_ADMIN";
	public static final String FIELDS = "fields";
	public static final String APPROVED = "APPROVED";
	public static final String SUPPORT_EMAIL = "supportEmail";
	public static final String LINK = "link";
	public static final String USER_NAME = "userName";
	public static final String VERIFIED_KARMAYOGI = "verifiedKarmayogi";
	public static final String SUCCESS = "success";
	public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
	public static final String EXCEPTION_MSG_DELETE = "Exception occurred while deleting record from ";
	public static final String INSERT_INTO = "INSERT INTO ";
	public static final String DOT = ".";
	public static final String OPEN_BRACE = "(";
	public static final String VALUES_WITH_BRACE = ") VALUES (";
	public static final String QUE_MARK = "?";
	public static final String COMMA = ",";
	public static final String CLOSING_BRACE = ");";

	public static final String KEYSPACE_SUNBIRD = "sunbird";
	public static final String KEYSPACE_SUNBIRD_COURSES = "sunbird_courses";
	public static final String TABLE_COURSE_BATCH = "course_batch";
	public static final String TABLE_ENROLMENT_BATCH_LOOKUP = "enrollment_batch_lookup";
	public static final String BATCH_ATTRIBUTES = "batchattributes";
	public static final String CURRENT_BATCH_SIZE = "currentBatchSize";
	public static final String BATCH_ID = "batchId";
	public static final String COURSE_ID = "courseId";
	public static final String ID = "id";
	public static final String ENROLLED_DATE = "enrolled_date";
	public static final String ENROLMENT_END_DATE = "enrollment_enddate";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
	public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_LOCAl = "maxConnectionsPerHostForLocal";
	public static final String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
	public static final String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
	public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
	public static final String POOL_TIMEOUT = "poolTimeoutMillis";
	public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
	public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "sunbird_cassandra_consistency_level";
	public static final String UNAUTHORIZED = "unauthorized";
	public static final String SEND_FOR_MDO_APPROVAL = "SEND_FOR_MDO_APPROVAL";
	public static final String SEND_FOR_PC_APPROVAL = "SEND_FOR_PC_APPROVAL";
	public static final String PROGRAM_COORDINATOR = "PROGRAM_COORDINATOR";
	public static final String TEMPLATE = "template";
	public static final String TABLE_EMAIL_TEMPLATE = "email_template";
	public static final String MISSING_APPLICATION_IDS_ERROR_MESSAGE = "Application Ids are missing in the request";
	public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
	public static final String TAG = "tag";
}
