package com.helpdesk.config;

import com.helpdesk.model.*;
import com.helpdesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(OrganizationRepository organizationRepository,
                           UserRepository userRepository,
                           SlaPolicyRepository slaPolicyRepository,
                           TicketRepository ticketRepository,
                           TicketCommentRepository commentRepository,
                           PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking HelpDesk Pro seed data initialization...");

        // Ensure Organizations
        Organization acme = organizationRepository.findBySlug("acme-corp").orElseGet(() ->
                organizationRepository.save(new Organization("Acme Corp", "acme-corp", "support@acme.internal")));

        Organization starlight = organizationRepository.findBySlug("starlight-logistics").orElseGet(() ->
                organizationRepository.save(new Organization("Starlight Logistics", "starlight-logistics", "help@starlight.internal")));

        // Ensure SLA Policies
        if (slaPolicyRepository.findByOrganizationId(acme.getId()).isEmpty()) {
            slaPolicyRepository.save(new SlaPolicy(acme, TicketPriority.URGENT, 1, 4));
            slaPolicyRepository.save(new SlaPolicy(acme, TicketPriority.HIGH, 2, 8));
            slaPolicyRepository.save(new SlaPolicy(acme, TicketPriority.MEDIUM, 4, 24));
            slaPolicyRepository.save(new SlaPolicy(acme, TicketPriority.LOW, 8, 48));
        }
        if (slaPolicyRepository.findByOrganizationId(starlight.getId()).isEmpty()) {
            slaPolicyRepository.save(new SlaPolicy(starlight, TicketPriority.URGENT, 1, 4));
            slaPolicyRepository.save(new SlaPolicy(starlight, TicketPriority.HIGH, 2, 8));
            slaPolicyRepository.save(new SlaPolicy(starlight, TicketPriority.MEDIUM, 4, 24));
            slaPolicyRepository.save(new SlaPolicy(starlight, TicketPriority.LOW, 8, 48));
        }

        // Ensure Users
        User superAdmin = userRepository.findByUsername("superadmin").orElseGet(() ->
                userRepository.save(new User("superadmin", "superadmin@helpdesk.platform", passwordEncoder.encode("Super@123"), "Super Admin", null, Role.ROLE_PLATFORM_ADMIN)));
        superAdmin.setPasswordHash(passwordEncoder.encode("Super@123"));
        userRepository.save(superAdmin);

        User acmeAdmin = userRepository.findByUsername("acme_admin").orElseGet(() ->
                userRepository.save(new User("acme_admin", "admin@acme.internal", passwordEncoder.encode("Admin@123"), "Acme Admin", acme, Role.ROLE_ORGANIZATION_ADMIN)));
        acmeAdmin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        userRepository.save(acmeAdmin);

        User acmeAgent = userRepository.findByUsername("acme_agent").orElseGet(() ->
                userRepository.save(new User("acme_agent", "agent@acme.internal", passwordEncoder.encode("Agent@123"), "Sarah Jenkins (Agent)", acme, Role.ROLE_AGENT)));
        acmeAgent.setPasswordHash(passwordEncoder.encode("Agent@123"));
        userRepository.save(acmeAgent);

        User acmeCustomer = userRepository.findByUsername("acme_customer").orElseGet(() ->
                userRepository.save(new User("acme_customer", "customer@client.com", passwordEncoder.encode("Customer@123"), "David Miller", acme, Role.ROLE_CUSTOMER)));
        acmeCustomer.setPasswordHash(passwordEncoder.encode("Customer@123"));
        userRepository.save(acmeCustomer);

        User starAdmin = userRepository.findByUsername("star_admin").orElseGet(() ->
                userRepository.save(new User("star_admin", "admin@starlight.internal", passwordEncoder.encode("Admin@123"), "Starlight Admin", starlight, Role.ROLE_ORGANIZATION_ADMIN)));
        starAdmin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        userRepository.save(starAdmin);

        User starCustomer = userRepository.findByUsername("star_customer").orElseGet(() ->
                userRepository.save(new User("star_customer", "ops@partner.com", passwordEncoder.encode("Customer@123"), "Emma Watson", starlight, Role.ROLE_CUSTOMER)));
        starCustomer.setPasswordHash(passwordEncoder.encode("Customer@123"));
        userRepository.save(starCustomer);

        // Ensure Initial Tickets
        if (ticketRepository.findByTicketNumber("HD-ACME-1001").isEmpty()) {
            Ticket t1 = new Ticket(
                    "HD-ACME-1001",
                    acme,
                    acmeCustomer,
                    "Cannot access billing invoices portal",
                    "When clicking download on the latest monthly invoice, a 403 error is received.",
                    TicketPriority.HIGH,
                    TicketCategory.BILLING,
                    Instant.now().plus(2, ChronoUnit.HOURS),
                    Instant.now().plus(8, ChronoUnit.HOURS)
            );
            t1.setAssignedAgent(acmeAgent);
            t1.setStatus(TicketStatus.ASSIGNED);
            ticketRepository.save(t1);

            commentRepository.save(new TicketComment(t1, acmeCustomer, "I verified this occurs on both Chrome and Firefox.", false));
            commentRepository.save(new TicketComment(t1, acmeAgent, "Internal note: checked backend S3 bucket permissions for org 1.", true));
        }

        if (ticketRepository.findByTicketNumber("HD-STAR-2001").isEmpty()) {
            Ticket t2 = new Ticket(
                    "HD-STAR-2001",
                    starlight,
                    starCustomer,
                    "Tracking webhook latency in EU region",
                    "Webhook events for container transit updates are delayed by 45 minutes.",
                    TicketPriority.MEDIUM,
                    TicketCategory.TECHNICAL,
                    Instant.now().plus(4, ChronoUnit.HOURS),
                    Instant.now().plus(24, ChronoUnit.HOURS)
            );
            ticketRepository.save(t2);
        }

        log.info("HelpDesk Pro initialization complete.");
    }
}
