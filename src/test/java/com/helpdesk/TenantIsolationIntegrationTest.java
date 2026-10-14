package com.helpdesk;

import com.helpdesk.dto.CreateCommentDto;
import com.helpdesk.dto.CreateTicketDto;
import com.helpdesk.dto.TicketResponseDto;
import com.helpdesk.exception.InvalidStatusTransitionException;
import com.helpdesk.exception.TenantAccessDeniedException;
import com.helpdesk.exception.UnauthorizedAttachmentAccessException;
import com.helpdesk.model.*;
import com.helpdesk.repository.*;
import com.helpdesk.service.AttachmentService;
import com.helpdesk.service.CommentService;
import com.helpdesk.service.CustomUserPrincipal;
import com.helpdesk.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class TenantIsolationIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    private CustomUserPrincipal acmeCustomerPrincipal;
    private CustomUserPrincipal acmeAgentPrincipal;
    private CustomUserPrincipal acmeAdminPrincipal;
    private CustomUserPrincipal starCustomerPrincipal;
    private CustomUserPrincipal starAdminPrincipal;

    private Ticket acmeTicket;
    private Ticket starTicket;

    @BeforeEach
    void setUp() {
        User acmeCustomer = userRepository.findByUsername("acme_customer").orElseThrow();
        User acmeAgent = userRepository.findByUsername("acme_agent").orElseThrow();
        User acmeAdmin = userRepository.findByUsername("acme_admin").orElseThrow();
        User starCustomer = userRepository.findByUsername("star_customer").orElseThrow();
        User starAdmin = userRepository.findByUsername("star_admin").orElseThrow();

        acmeCustomerPrincipal = new CustomUserPrincipal(acmeCustomer);
        acmeAgentPrincipal = new CustomUserPrincipal(acmeAgent);
        acmeAdminPrincipal = new CustomUserPrincipal(acmeAdmin);
        starCustomerPrincipal = new CustomUserPrincipal(starCustomer);
        starAdminPrincipal = new CustomUserPrincipal(starAdmin);

        acmeTicket = ticketRepository.findByTicketNumber("HD-ACME-1001").orElseThrow();
        starTicket = ticketRepository.findByTicketNumber("HD-STAR-2001").orElseThrow();
    }

    @Test
    @DisplayName("Cross-tenant violation: User from Org 2 cannot read tickets from Org 1")
    void testCrossOrganizationAccessDenied_Customer() {
        // Starlight customer attempts to access Acme ticket
        assertThrows(TenantAccessDeniedException.class, () ->
                ticketService.getTicketDto(acmeTicket.getId(), starCustomerPrincipal)
        );
    }

    @Test
    @DisplayName("Cross-tenant violation: Admin from Org 2 cannot transition status of tickets from Org 1")
    void testCrossOrganizationAccessDenied_Admin() {
        // Starlight admin attempts to transition status on Acme ticket
        assertThrows(TenantAccessDeniedException.class, () ->
                ticketService.transitionStatus(acmeTicket.getId(), TicketStatus.RESOLVED, "Cross-org hack", starAdminPrincipal)
        );
    }

    @Test
    @DisplayName("Internal note privacy: Customers must NOT see internal staff notes")
    void testCustomerCannotSeeInternalNotes() {
        // Staff adds an internal note
        CreateCommentDto noteDto = new CreateCommentDto("Confidential infrastructure diagnostics", true);
        commentService.addComment(acmeTicket.getId(), noteDto, acmeAgentPrincipal);

        // Staff views comments: must see all comments including internal note
        var staffComments = commentService.getCommentsForTicket(acmeTicket.getId(), acmeAgentPrincipal);
        boolean staffHasNote = staffComments.stream().anyMatch(c -> c.isInternalNote() && c.getContent().contains("Confidential"));
        assertThat(staffHasNote).isTrue();

        // Customer views comments: internal notes MUST be excluded!
        var customerComments = commentService.getCommentsForTicket(acmeTicket.getId(), acmeCustomerPrincipal);
        boolean customerHasNote = customerComments.stream().anyMatch(c -> c.isInternalNote() || c.getContent().contains("Confidential"));
        assertThat(customerHasNote).isFalse();
    }

    @Test
    @DisplayName("Ticket workflow: Valid state machine transitions succeed in sequence")
    void testValidWorkflowTransitions_Success() {
        // Create fresh ticket in OPEN status
        CreateTicketDto newDto = new CreateTicketDto("VPN tunnel failure", "Cannot route traffic to db subnet", TicketPriority.HIGH, TicketCategory.TECHNICAL);
        TicketResponseDto created = ticketService.createTicket(newDto, acmeCustomerPrincipal);
        assertThat(created.getStatus()).isEqualTo("OPEN");

        // 1. OPEN -> ASSIGNED
        User agent = userRepository.findByUsername("acme_agent").orElseThrow();
        TicketResponseDto assigned = ticketService.assignTicket(created.getId(), agent.getId(), acmeAdminPrincipal);
        assertThat(assigned.getStatus()).isEqualTo("ASSIGNED");

        // 2. ASSIGNED -> IN_PROGRESS
        TicketResponseDto inProgress = ticketService.transitionStatus(created.getId(), TicketStatus.IN_PROGRESS, "Investigating network", acmeAgentPrincipal);
        assertThat(inProgress.getStatus()).isEqualTo("IN_PROGRESS");

        // 3. IN_PROGRESS -> RESOLVED
        TicketResponseDto resolved = ticketService.transitionStatus(created.getId(), TicketStatus.RESOLVED, "BGP route re-advertised", acmeAgentPrincipal);
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");

        // 4. RESOLVED -> CLOSED
        TicketResponseDto closed = ticketService.transitionStatus(created.getId(), TicketStatus.CLOSED, "Confirmed by user", acmeCustomerPrincipal);
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("Ticket workflow: Arbitrary invalid state transition is prohibited")
    void testInvalidWorkflowTransition_ThrowsException() {
        CreateTicketDto newDto = new CreateTicketDto("Printer offline", "Office 2nd floor", TicketPriority.LOW, TicketCategory.TECHNICAL);
        TicketResponseDto created = ticketService.createTicket(newDto, acmeCustomerPrincipal);

        // OPEN cannot jump directly to RESOLVED without assignment / progress
        assertThrows(InvalidStatusTransitionException.class, () ->
                ticketService.transitionStatus(created.getId(), TicketStatus.RESOLVED, "Skipping workflow", acmeAgentPrincipal)
        );
    }

    @Test
    @DisplayName("Role security: Customer cannot perform staff status transitions")
    void testCustomerCannotTransitionToStaffStatuses() {
        // Customer cannot mark their own ticket as IN_PROGRESS
        assertThrows(InvalidStatusTransitionException.class, () ->
                ticketService.transitionStatus(acmeTicket.getId(), TicketStatus.IN_PROGRESS, "Customer hacking status", acmeCustomerPrincipal)
        );
    }

    @Test
    @DisplayName("Tenant isolation: Cannot assign ticket to an agent from another organization")
    void testCrossOrganizationAssignmentDenied() {
        User starAdminAsAgent = userRepository.findByUsername("star_admin").orElseThrow();

        // Acme admin attempts to assign Acme ticket to Starlight admin
        assertThrows(TenantAccessDeniedException.class, () ->
                ticketService.assignTicket(acmeTicket.getId(), starAdminAsAgent.getId(), acmeAdminPrincipal)
        );
    }

    @Test
    @DisplayName("Attachment security: Customer cannot download attachment linked to internal note")
    void testUnauthorizedInternalNoteAttachmentAccess() {
        User staff = userRepository.findByUsername("acme_agent").orElseThrow();
        TicketComment internalComment = commentRepository.save(new TicketComment(acmeTicket, staff, "Private log dump", true));

        Attachment internalAttachment = new Attachment(
                acmeTicket,
                internalComment,
                staff,
                "private_dump.log",
                "safe_uuid_private_dump.log",
                1024L,
                "text/plain"
        );
        attachmentRepository.save(internalAttachment);

        // Customer attempts to download internal note attachment
        assertThrows(UnauthorizedAttachmentAccessException.class, () ->
                attachmentService.loadAttachmentAsResource(internalAttachment.getId(), acmeCustomerPrincipal)
        );
    }
}
