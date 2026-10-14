package com.helpdesk.repository;

import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<Ticket> findByTicketNumberAndOrganizationId(String ticketNumber, Long organizationId);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Page<Ticket> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndStatus(Long organizationId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndCustomerId(Long organizationId, Long customerId, Pageable pageable);

    Page<Ticket> findByOrganizationIdAndAssignedAgentId(Long organizationId, Long agentId, Pageable pageable);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, TicketStatus status);

    long countByOrganizationIdAndResolutionBreachedTrue(Long organizationId);

    @Query("SELECT t FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') AND t.firstResponseDeadline < :now AND t.firstResponseBreached = false AND t.firstRespondedAt IS NULL")
    List<Ticket> findBreachedFirstResponseTickets(@Param("now") Instant now);

    @Query("SELECT t FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') AND t.resolutionDeadline < :now AND t.resolutionBreached = false")
    List<Ticket> findBreachedResolutionTickets(@Param("now") Instant now);
}
