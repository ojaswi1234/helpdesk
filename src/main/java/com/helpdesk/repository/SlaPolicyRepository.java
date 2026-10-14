package com.helpdesk.repository;

import com.helpdesk.model.SlaPolicy;
import com.helpdesk.model.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByOrganizationIdAndPriority(Long organizationId, TicketPriority priority);
    List<SlaPolicy> findByOrganizationId(Long organizationId);
}
