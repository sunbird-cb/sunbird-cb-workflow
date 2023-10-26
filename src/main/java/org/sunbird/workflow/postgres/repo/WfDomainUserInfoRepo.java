package org.sunbird.workflow.postgres.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sunbird.workflow.postgres.entity.WfDomainLookup;
import org.sunbird.workflow.postgres.entity.WfDomainUserInfo;

import java.util.List;

public interface WfDomainUserInfoRepo extends JpaRepository<WfDomainUserInfo, Integer> {
    List<WfDomainUserInfo> findByDomainName(String domainValue);

    List<WfDomainUserInfo> findByDomainNameAndEmailAndMobile(String domainValue, String email, String mobile);
}
