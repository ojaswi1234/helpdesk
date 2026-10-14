package com.helpdesk.service;

import com.helpdesk.dto.CommentResponseDto;
import com.helpdesk.dto.CreateCommentDto;
import com.helpdesk.event.TicketEvent;
import com.helpdesk.exception.TenantAccessDeniedException;
import com.helpdesk.model.Role;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketComment;
import com.helpdesk.model.User;
import com.helpdesk.repository.TicketCommentRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(TicketCommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          TicketService ticketService,
                          ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponseDto addComment(Long ticketId, CreateCommentDto dto, CustomUserPrincipal principal) {
        Ticket ticket = ticketService.getTicketEntityAndVerifyTenant(ticketId, principal);
        User author = userRepository.findById(principal.getUserId()).orElseThrow();

        boolean internalNote = dto.isInternalNote();
        if (internalNote && !principal.isStaff()) {
            throw new TenantAccessDeniedException("Customers cannot create internal staff notes.");
        }

        TicketComment comment = new TicketComment(
                ticket,
                author,
                dto.getContent().trim(),
                internalNote
        );
        TicketComment saved = commentRepository.save(comment);

        // If staff response and ticket hasn't recorded first response yet, record it!
        if (principal.isStaff() && !internalNote && ticket.getFirstRespondedAt() == null) {
            ticket.setFirstRespondedAt(Instant.now());
            ticketRepository.save(ticket);
        }

        // Publish event for AFTER_COMMIT WebSocket delivery (exclude internal notes from customer updates)
        if (!internalNote) {
            eventPublisher.publishEvent(new TicketEvent(
                    "NEW_COMMENT",
                    ticket.getId(),
                    ticket.getTicketNumber(),
                    ticket.getOrganization().getId(),
                    "New Public Comment",
                    "New comment on ticket " + ticket.getTicketNumber() + " by " + author.getFullName(),
                    ticket.getStatus().name(),
                    ticket.getCustomer().getUsername(),
                    ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getUsername() : null,
                    author.getUsername()
            ));
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsForTicket(Long ticketId, CustomUserPrincipal principal) {
        ticketService.getTicketEntityAndVerifyTenant(ticketId, principal);

        List<TicketComment> comments;
        if (principal.getRole() == Role.ROLE_CUSTOMER) {
            // Strict security filter: Customers MUST NOT see internal notes
            comments = commentRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        } else {
            comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        }

        return comments.stream().map(this::toDto).collect(Collectors.toList());
    }

    private CommentResponseDto toDto(TicketComment c) {
        return new CommentResponseDto(
                c.getId(),
                c.getTicket().getId(),
                c.getUser().getId(),
                c.getUser().getUsername(),
                c.getUser().getFullName(),
                c.getUser().getRole().name(),
                c.getContent(),
                c.isInternalNote(),
                c.getCreatedAt()
        );
    }
}
