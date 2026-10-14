package com.helpdesk.service;

import com.helpdesk.model.AuditEvent;
import com.helpdesk.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void recordEvent(Long organizationId, Long userId, String username, String action, String resourceType, String resourceId, String details) {
        AuditEvent event = new AuditEvent(organizationId, userId, username, action, resourceType, resourceId, details);
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAuditLogsForOrg(Long organizationId) {
        return auditEventRepository.findTop50ByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
}
