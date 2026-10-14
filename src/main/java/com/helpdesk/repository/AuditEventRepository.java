package com.helpdesk.repository;

import com.helpdesk.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop50ByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    Page<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);
}
