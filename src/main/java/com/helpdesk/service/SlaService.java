package com.helpdesk.service;

import com.helpdesk.event.TicketEvent;
import com.helpdesk.model.Organization;
import com.helpdesk.model.SlaPolicy;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketPriority;
import com.helpdesk.repository.SlaPolicyRepository;
import com.helpdesk.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SlaService(SlaPolicyRepository slaPolicyRepository,
                      TicketRepository ticketRepository,
                      ApplicationEventPublisher eventPublisher) {
        this.slaPolicyRepository = slaPolicyRepository;
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    public static class SlaDeadlines {
        private final Instant firstResponseDeadline;
        private final Instant resolutionDeadline;

        public SlaDeadlines(Instant firstResponseDeadline, Instant resolutionDeadline) {
            this.firstResponseDeadline = firstResponseDeadline;
            this.resolutionDeadline = resolutionDeadline;
        }

        public Instant getFirstResponseDeadline() {
            return firstResponseDeadline;
        }

        public Instant getResolutionDeadline() {
            return resolutionDeadline;
        }
    }

    @Transactional(readOnly = true)
    public SlaDeadlines calculateDeadlines(Organization organization, TicketPriority priority) {
        int firstResponseHours = priority.getDefaultFirstResponseHours();
        int resolutionHours = priority.getDefaultResolutionHours();

        if (organization != null) {
            var policyOpt = slaPolicyRepository.findByOrganizationIdAndPriority(organization.getId(), priority);
            if (policyOpt.isPresent()) {
                SlaPolicy policy = policyOpt.get();
                firstResponseHours = policy.getFirstResponseHours();
                resolutionHours = policy.getResolutionHours();
            }
        }

        Instant now = Instant.now();
        Instant responseDeadline = now.plus(firstResponseHours, ChronoUnit.HOURS);
        Instant resDeadline = now.plus(resolutionHours, ChronoUnit.HOURS);

        return new SlaDeadlines(responseDeadline, resDeadline);
    }

    @Scheduled(fixedRate = 60000) // Check every 60 seconds
    @Transactional
    public void evaluateSlaBreaches() {
        Instant now = Instant.now();

        // 1. Check First Response Breaches
        List<Ticket> breachedFirstResponses = ticketRepository.findBreachedFirstResponseTickets(now);
        for (Ticket ticket : breachedFirstResponses) {
            ticket.setFirstResponseBreached(true);
            ticketRepository.save(ticket);
            log.warn("SLA ALERT: First response breached for ticket: {} (Org: {})", ticket.getTicketNumber(), ticket.getOrganization().getName());

            eventPublisher.publishEvent(new TicketEvent(
                    "SLA_BREACH",
                    ticket.getId(),
                    ticket.getTicketNumber(),
                    ticket.getOrganization().getId(),
                    "SLA First Response Breached",
                    "Ticket " + ticket.getTicketNumber() + " has exceeded its first response target deadline.",
                    ticket.getStatus().name(),
                    ticket.getCustomer().getUsername(),
                    ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getUsername() : null,
                    "SYSTEM_SLA_ENGINE"
            ));
        }

        // 2. Check Resolution Breaches
        List<Ticket> breachedResolutions = ticketRepository.findBreachedResolutionTickets(now);
        for (Ticket ticket : breachedResolutions) {
            ticket.setResolutionBreached(true);
            ticketRepository.save(ticket);
            log.warn("SLA ALERT: Resolution deadline breached for ticket: {} (Org: {})", ticket.getTicketNumber(), ticket.getOrganization().getName());

            eventPublisher.publishEvent(new TicketEvent(
                    "SLA_BREACH",
                    ticket.getId(),
                    ticket.getTicketNumber(),
                    ticket.getOrganization().getId(),
                    "SLA Resolution Target Breached",
                    "Ticket " + ticket.getTicketNumber() + " has exceeded its overall resolution deadline.",
                    ticket.getStatus().name(),
                    ticket.getCustomer().getUsername(),
                    ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getUsername() : null,
                    "SYSTEM_SLA_ENGINE"
            ));
        }
    }
}
