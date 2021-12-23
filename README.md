# Workflow-Service

This service provides the work-flow features to handle request which requires Administrator approval.

## Tech

SB-CB-WORKFLOW uses a number of open source projects:

- [APACHE KAFKA] - used for data pipeline and data integration
- [SPRING BOOT] - Great framework to work with java.
- [JAVA] - used for core development
- [POSTGRESQL] - a relational database

**Postgresql table list**

- wf_status
- wf_audit

**Queries to create the tables**

```sh
CREATE TABLE if not exists wf_status
(   
    wf_id character varying(64) NOT NULL,
    application_id character varying(64) NOT NULL,
    userid character varying(64) NOT NULL,
    in_workflow boolean,
    service_name character varying(64),
    actor_uuid character varying(64),
    created_on timestamp without time zone,
    current_status character varying(64),
    lastupdated_on timestamp without time zone,
    org character varying(64) NOT NULL,
    root_org character varying(64) NOT NULL,
    update_field_values character varying(1024),
    CONSTRAINT userprofile_wf_status_pkey PRIMARY KEY (wf_id)
);
```
```sh

CREATE SEQUENCE wf_audit_id_seq
INCREMENT 1
START 1
MINVALUE 1
MAXVALUE 9223372036854775807
CACHE 1;
```
```sh

CREATE TABLE wf_audit
(
id integer NOT NULL DEFAULT nextval('wingspan.wf_audit_id_seq'::regclass),
wf_id character varying(64),
application_id character varying(64) NOT NULL,
actor_uuid character varying(64),
service_name character varying(64),
update_field_values character varying(1024),
comment character varying(1024),
created_on timestamp without time zone,
action character varying(64),
state character varying(64),
root_org character varying(64),
user_id character varying(64),
in_workflow boolean,
CONSTRAINT user_profile_wf_audit_pkey PRIMARY KEY (id)
);
```
**Postman Collection**

https://www.getpostman.com/collections/93d2deb377ef3edb3a22
