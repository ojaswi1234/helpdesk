package com.helpdesk;

import com.helpdesk.model.Organization;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketCategory;
import com.helpdesk.model.TicketPriority;
import com.helpdesk.model.User;
import com.helpdesk.repository.OrganizationRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.SlaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SlaServiceTest {

    @Autowired
    private SlaService slaService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should correctly calculate SLA deadlines based on priority and policy")
    void testCalculateDeadlines_UrgentAndHigh() {
        Organization org = organizationRepository.findBySlug("acme-corp").orElseThrow();

        // URGENT: 1h response, 4h resolution
        SlaService.SlaDeadlines urgentDeadlines = slaService.calculateDeadlines(org, TicketPriority.URGENT);
        long responseDiffHours = ChronoUnit.HOURS.between(Instant.now(), urgentDeadlines.getFirstResponseDeadline());
        long resDiffHours = ChronoUnit.HOURS.between(Instant.now(), urgentDeadlines.getResolutionDeadline());

        assertThat(responseDiffHours).isBetween(0L, 1L);
        assertThat(resDiffHours).isBetween(3L, 4L);

        // HIGH: 2h response, 8h resolution
        SlaService.SlaDeadlines highDeadlines = slaService.calculateDeadlines(org, TicketPriority.HIGH);
        long highResponseDiff = ChronoUnit.HOURS.between(Instant.now(), highDeadlines.getFirstResponseDeadline());
        long highResDiff = ChronoUnit.HOURS.between(Instant.now(), highDeadlines.getResolutionDeadline());

        assertThat(highResponseDiff).isBetween(1L, 2L);
        assertThat(highResDiff).isBetween(7L, 8L);
    }

    @Test
    @DisplayName("Should detect and flag breached tickets during scheduled SLA evaluation")
    void testEvaluateSlaBreaches_FlagsPastDueTicket() {
        Organization org = organizationRepository.findBySlug("acme-corp").orElseThrow();
        User customer = userRepository.findByUsername("acme_customer").orElseThrow();

        // Create an overdue ticket with deadline in the past
        Ticket overdueTicket = new Ticket(
                "HD-OVERDUE-" + System.currentTimeMillis(),
                org,
                customer,
                "Overdue server crash",
                "Past SLA deadline",
                TicketPriority.URGENT,
                TicketCategory.TECHNICAL,
                Instant.now().minus(2, ChronoUnit.HOURS), // breached response
                Instant.now().minus(1, ChronoUnit.HOURS)  // breached resolution
        );
        ticketRepository.save(overdueTicket);

        // Run scheduled SLA check
        slaService.evaluateSlaBreaches();

        Ticket updated = ticketRepository.findById(overdueTicket.getId()).orElseThrow();
        assertThat(updated.isFirstResponseBreached()).isTrue();
        assertThat(updated.isResolutionBreached()).isTrue();
    }
}
