package com.helpdesk.service;

import com.helpdesk.dto.CreateTicketDto;
import com.helpdesk.dto.TicketResponseDto;
import com.helpdesk.event.TicketEvent;
import com.helpdesk.exception.InvalidStatusTransitionException;
import com.helpdesk.exception.TenantAccessDeniedException;
import com.helpdesk.exception.TicketNotFoundException;
import com.helpdesk.model.*;
import com.helpdesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final SlaService slaService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public TicketService(TicketRepository ticketRepository,
                         OrganizationRepository organizationRepository,
                         UserRepository userRepository,
                         TicketStatusHistoryRepository statusHistoryRepository,
                         SlaService slaService,
                         NotificationService notificationService,
                         AuditService auditService,
                         ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.slaService = slaService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TicketResponseDto createTicket(CreateTicketDto dto, CustomUserPrincipal principal) {
        if (principal.getOrganizationId() == null) {
            throw new TenantAccessDeniedException("Platform administrators cannot file organization tickets directly without an organization context.");
        }

        Organization org = organizationRepository.findById(principal.getOrganizationId())
                .orElseThrow(() -> new TenantAccessDeniedException("Organization not found: " + principal.getOrganizationId()));

        User customer = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new TicketNotFoundException("Customer user not found: " + principal.getUserId()));

        // Calculate SLA deadlines
        SlaService.SlaDeadlines deadlines = slaService.calculateDeadlines(org, dto.getPriority());

        String prefix = org.getSlug().toUpperCase().replace("-", "");
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        String ticketNumber = "HD-" + prefix + "-" + (1000 + random.nextInt(9000));

        Ticket ticket = new Ticket(
                ticketNumber,
                org,
                customer,
                dto.getTitle().trim(),
                dto.getDescription().trim(),
                dto.getPriority(),
                dto.getCategory(),
                deadlines.getFirstResponseDeadline(),
                deadlines.getResolutionDeadline()
        );

        Ticket saved = ticketRepository.save(ticket);

        // Record initial status history
        TicketStatusHistory history = new TicketStatusHistory(saved, customer, null, TicketStatus.OPEN, "Ticket submitted by customer");
        statusHistoryRepository.save(history);

        // Audit Event
        auditService.recordEvent(org.getId(), customer.getId(), customer.getUsername(), "TICKET_CREATED", "TICKET", ticketNumber, "Priority: " + dto.getPriority());

        // Publish event for AFTER_COMMIT WebSocket delivery
        eventPublisher.publishEvent(new TicketEvent(
                "TICKET_CREATED",
                saved.getId(),
                saved.getTicketNumber(),
                org.getId(),
                "New Support Ticket Created",
                String.format("[%s] %s (%s)", saved.getTicketNumber(), saved.getTitle(), saved.getPriority()),
                saved.getStatus().name(),
                customer.getUsername(),
                null,
                customer.getUsername()
        ));

        log.info("Ticket created: {} for org: {} by {}", saved.getTicketNumber(), org.getName(), customer.getUsername());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Ticket getTicketEntityAndVerifyTenant(Long ticketId, CustomUserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with ID: " + ticketId));

        verifyTenantAccess(ticket, principal);
        return ticket;
    }

    @Transactional(readOnly = true)
    public TicketResponseDto getTicketDto(Long ticketId, CustomUserPrincipal principal) {
        Ticket ticket = getTicketEntityAndVerifyTenant(ticketId, principal);
        return toDto(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponseDto> getTickets(CustomUserPrincipal principal, TicketStatus statusFilter, Pageable pageable) {
        Page<Ticket> page;

        if (principal.isPlatformAdmin()) {
            page = ticketRepository.findAll(pageable);
        } else if (principal.getRole() == Role.ROLE_CUSTOMER) {
            // Customers only see their own tickets inside their organization
            page = ticketRepository.findByOrganizationIdAndCustomerId(principal.getOrganizationId(), principal.getUserId(), pageable);
        } else {
            // Staff / Agents / Org Admins see all organization tickets
            if (statusFilter != null) {
                page = ticketRepository.findByOrganizationIdAndStatus(principal.getOrganizationId(), statusFilter, pageable);
            } else {
                page = ticketRepository.findByOrganizationId(principal.getOrganizationId(), pageable);
            }
        }

        return page.map(this::toDto);
    }

    @Transactional
    public TicketResponseDto transitionStatus(Long ticketId, TicketStatus targetStatus, String reason, CustomUserPrincipal principal) {
        Ticket ticket = getTicketEntityAndVerifyTenant(ticketId, principal);
        TicketStatus current = ticket.getStatus();

        // 1. Validate status transition graph
        if (!current.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid state transition from %s to %s for ticket %s", current, targetStatus, ticket.getTicketNumber())
            );
        }

        // 2. Validate customer permissions (customers can only close or reopen their own ticket)
        if (principal.getRole() == Role.ROLE_CUSTOMER) {
            if (targetStatus != TicketStatus.CLOSED && targetStatus != TicketStatus.OPEN) {
                throw new InvalidStatusTransitionException("Customers can only close or reopen resolved tickets.");
            }
        }

        User user = userRepository.findById(principal.getUserId()).orElseThrow();
        ticket.setStatus(targetStatus);
        if (targetStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }

        ticketRepository.save(ticket);

        // Record History
        TicketStatusHistory history = new TicketStatusHistory(ticket, user, current, targetStatus, reason);
        statusHistoryRepository.save(history);

        // Audit Event
        auditService.recordEvent(ticket.getOrganization().getId(), user.getId(), user.getUsername(), "STATUS_TRANSITION", "TICKET", ticket.getTicketNumber(), current + " -> " + targetStatus);

        // Publish event for AFTER_COMMIT WebSocket delivery
        eventPublisher.publishEvent(new TicketEvent(
                "STATUS_CHANGED",
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getOrganization().getId(),
                "Ticket Status Updated",
                String.format("Ticket %s changed status from %s to %s", ticket.getTicketNumber(), current, targetStatus),
                targetStatus.name(),
                ticket.getCustomer().getUsername(),
                ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getUsername() : null,
                user.getUsername()
        ));

        return toDto(ticket);
    }

    @Transactional
    public TicketResponseDto assignTicket(Long ticketId, Long agentId, CustomUserPrincipal principal) {
        if (!principal.isStaff()) {
            throw new TenantAccessDeniedException("Only support staff or organization administrators can assign tickets.");
        }

        Ticket ticket = getTicketEntityAndVerifyTenant(ticketId, principal);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new TicketNotFoundException("Agent not found with ID: " + agentId));

        // Enforce agent organization boundary
        if (!principal.isPlatformAdmin() && !agent.getOrganization().getId().equals(ticket.getOrganization().getId())) {
            throw new TenantAccessDeniedException("Cannot assign ticket to an agent outside the ticket's organization.");
        }

        ticket.setAssignedAgent(agent);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
            TicketStatusHistory history = new TicketStatusHistory(ticket, agent, TicketStatus.OPEN, TicketStatus.ASSIGNED, "Assigned to " + agent.getFullName());
            statusHistoryRepository.save(history);
        }

        ticketRepository.save(ticket);

        // Notify Agent
        notificationService.createNotification(
                agent,
                ticket.getOrganization(),
                "Ticket Assigned",
                "You have been assigned ticket " + ticket.getTicketNumber() + ": " + ticket.getTitle(),
                "/tickets/" + ticket.getId()
        );

        // Publish event for AFTER_COMMIT WebSocket delivery
        eventPublisher.publishEvent(new TicketEvent(
                "TICKET_ASSIGNED",
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getOrganization().getId(),
                "Ticket Assigned",
                String.format("Ticket %s assigned to %s", ticket.getTicketNumber(), agent.getFullName()),
                ticket.getStatus().name(),
                ticket.getCustomer().getUsername(),
                agent.getUsername(),
                principal.getUsername()
        ));

        return toDto(ticket);
    }

    public void verifyTenantAccess(Ticket ticket, CustomUserPrincipal principal) {
        if (principal.isPlatformAdmin()) {
            return;
        }
        if (principal.getOrganizationId() == null || !principal.getOrganizationId().equals(ticket.getOrganization().getId())) {
            log.warn("SECURITY ALERT: User {} from Org {} attempted unauthorized access to ticket {} belonging to Org {}",
                    principal.getUsername(), principal.getOrganizationId(), ticket.getTicketNumber(), ticket.getOrganization().getId());
            throw new TenantAccessDeniedException("Access Denied: Ticket " + ticket.getTicketNumber() + " belongs to a different organization.");
        }
    }

    public TicketResponseDto toDto(Ticket t) {
        TicketResponseDto dto = new TicketResponseDto();
        dto.setId(t.getId());
        dto.setTicketNumber(t.getTicketNumber());
        dto.setOrganizationId(t.getOrganization().getId());
        dto.setOrganizationName(t.getOrganization().getName());
        dto.setCustomerId(t.getCustomer().getId());
        dto.setCustomerUsername(t.getCustomer().getUsername());
        dto.setCustomerFullName(t.getCustomer().getFullName());
        if (t.getAssignedAgent() != null) {
            dto.setAssignedAgentId(t.getAssignedAgent().getId());
            dto.setAssignedAgentName(t.getAssignedAgent().getFullName());
        }
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setPriority(t.getPriority().name());
        dto.setCategory(t.getCategory().name());
        dto.setStatus(t.getStatus().name());
        dto.setFirstResponseDeadline(t.getFirstResponseDeadline());
        dto.setResolutionDeadline(t.getResolutionDeadline());
        dto.setFirstRespondedAt(t.getFirstRespondedAt());
        dto.setResolvedAt(t.getResolvedAt());
        dto.setFirstResponseBreached(t.isFirstResponseBreached());
        dto.setResolutionBreached(t.isResolutionBreached());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
