package org.sunbird.workflow.postgres.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.sunbird.workflow.postgres.entity.WfDomainLookup;

import java.util.List;

public interface WfDomainLookupRepo  extends JpaRepository<WfDomainLookup, Integer> {
    List<WfDomainLookup> findByDomainName(String domainValue);
}
