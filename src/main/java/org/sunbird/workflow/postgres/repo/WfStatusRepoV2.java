package org.sunbird.workflow.postgres.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;

import java.util.List;

@Repository
public interface WfStatusRepoV2 extends JpaRepository<WfStatusEntityV2 , String> {
    WfStatusEntityV2 findByUserIdAndWfId(String userId, String wfId);
    Page<WfStatusEntityV2> findByServiceNameAndCurrentStatus(String servicename , String status, Pageable pageable);
}
