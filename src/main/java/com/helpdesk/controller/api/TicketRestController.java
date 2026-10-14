package com.helpdesk.controller.api;

import com.helpdesk.dto.*;
import com.helpdesk.model.TicketStatus;
import com.helpdesk.service.CommentService;
import com.helpdesk.service.CustomUserPrincipal;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketRestController {

    private final TicketService ticketService;
    private final CommentService commentService;

    public TicketRestController(TicketService ticketService, CommentService commentService) {
        this.ticketService = ticketService;
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponseDto>> getTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Page<TicketResponseDto> tickets = ticketService.getTickets(
                principal,
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(tickets);
    }

    @PostMapping
    public ResponseEntity<TicketResponseDto> createTicket(
            @Valid @RequestBody CreateTicketDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        TicketResponseDto response = ticketService.createTicket(dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDto> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        TicketResponseDto ticket = ticketService.getTicketDto(id, principal);
        return ResponseEntity.ok(ticket);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        TicketResponseDto response = ticketService.transitionStatus(id, dto.getTargetStatus(), dto.getReason(), principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDto>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        List<CommentResponseDto> comments = commentService.getCommentsForTicket(id, principal);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponseDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        CommentResponseDto response = commentService.addComment(id, dto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
