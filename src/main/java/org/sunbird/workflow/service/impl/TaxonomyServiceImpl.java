package org.sunbird.workflow.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sunbird.workflow.core.WFLogger;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.WfServiceHandler;

import java.util.Objects;

@Service("taxonomyServiceImpl")
public class TaxonomyServiceImpl implements WfServiceHandler {

    private WFLogger logger = new WFLogger("TaxonomyServiceImpl");

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Override
    @Transactional
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest.getUserId()) && Objects.nonNull(wfRequest.getState())){
            wfStatusRepo.findByUserId(wfRequest.getUserId(),wfRequest.getState());
        }
    }
}
