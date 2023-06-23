package org.sunbird.workflow.postgres.repo;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;

import java.util.List;

@Repository
public interface WfStatusRepo extends JpaRepository<WfStatusEntity, String> {

    WfStatusEntity findByRootOrgAndOrgAndApplicationIdAndWfId(String rootOrg, String org, String applicationId, String wfId);

    Page<WfStatusEntity> findByRootOrgAndOrgAndServiceNameAndCurrentStatus(String rootOrg, String org, String servicename , String status, Pageable pageable);

    WfStatusEntity findByApplicationIdAndWfId(String applicationId, String wfId);

    WfStatusEntity findByRootOrgAndWfId(String rootOrg, String wfId);

    @Query(value = "select application_id from wingspan.wf_status where root_org= ?1 and service_name = ?2 and current_status = ?3 group by application_id", countQuery = "select count(application_id) from wingspan.wf_status where root_org= ?1 and service_name = ?2 and current_status = ?3 group by application_id", nativeQuery = true)
    List<String> getListOfDistinctApplication(String rootOrg, String serviceName, String currentStatus, Pageable pageable);
    
    @Query(value = "select application_id from wingspan.wf_status where root_org= ?1 and service_name = ?2 and current_status = ?3 and dept_name = ?4 group by application_id", countQuery = "select count(application_id) from wingspan.wf_status where root_org= ?1 and service_name = ?2 and current_status = ?3 group by application_id", nativeQuery = true)
    List<String> getListOfDistinctApplicationUsingDept(String rootOrg, String serviceName, String currentStatus, String deptName, Pageable pageable);
    
    List<WfStatusEntity> findByServiceNameAndCurrentStatusAndApplicationIdIn(String serviceName, String currentStatus, List<String> applicationId);
    
    List<WfStatusEntity> findByServiceNameAndCurrentStatusAndDeptNameAndApplicationIdIn(String serviceName, String currentStatus, String deptName, List<String> applicationId);

    List<WfStatusEntity> findByRootOrgAndOrgAndServiceNameAndCurrentStatusAndUserId(String rootOrg, String org, String servicename , String status, String userId);
    
    @Query(value = "select update_field_values from wingspan.wf_status where root_org= ?1 and org = ?2 and service_name = ?3 and current_status = ?4 and userid = ?5", nativeQuery = true)	
    List<String> findWfFieldsForUser(String rootOrg, String org, String servicename , String status, String userId);

    @Query(value = "select * from wingspan.wf_status where service_name= ?1 and current_status = ?2 and dept_Name = ?3", nativeQuery = true)
    List<WfStatusEntity> findByServiceNameAndCurrentStatusAndDeptName(String serviceName, String currentStatus, String deptName);

    @Query(value = "select * from wingspan.wf_status where service_name = ?1 and userid = ?2 and application_id IN ?3", nativeQuery = true)
    List<WfStatusEntity> findByServiceNameAndUserIdAndApplicationIdIn(String serviceName, String userId, List<String> applicationIds);

    @Query(value = "select * from wingspan.wf_status where service_name= ?1 and current_status = ?2 and dept_Name = ?3 and application_id IN ?3", nativeQuery = true)
    List<WfStatusEntity> findByServiceNameAndCurrentStatusAndDeptNameAndApplicationId(String serviceName, String currentStatus, String deptName, List<String> applicationIds);

    WfStatusEntity findByWfId(String wfId);
}
