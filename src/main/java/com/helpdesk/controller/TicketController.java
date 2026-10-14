package com.helpdesk.controller;

import com.helpdesk.dto.CreateCommentDto;
import com.helpdesk.dto.CreateTicketDto;
import com.helpdesk.dto.TicketResponseDto;
import com.helpdesk.exception.HelpDeskException;
import com.helpdesk.model.Role;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketCategory;
import com.helpdesk.model.TicketPriority;
import com.helpdesk.model.TicketStatus;
import com.helpdesk.model.User;
import com.helpdesk.repository.TicketStatusHistoryRepository;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.AttachmentService;
import com.helpdesk.service.CommentService;
import com.helpdesk.service.CustomUserPrincipal;
import com.helpdesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;
    private final AttachmentService attachmentService;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    public TicketController(TicketService ticketService,
                            CommentService commentService,
                            AttachmentService attachmentService,
                            TicketStatusHistoryRepository statusHistoryRepository,
                            UserRepository userRepository) {
        this.ticketService = ticketService;
        this.commentService = commentService;
        this.attachmentService = attachmentService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/tickets")
    public String listTickets(@RequestParam(required = false) TicketStatus status,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @AuthenticationPrincipal CustomUserPrincipal principal,
                              Model model) {
        Page<TicketResponseDto> ticketPage = ticketService.getTickets(
                principal,
                status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );

        model.addAttribute("ticketPage", ticketPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketPage.getTotalPages());
        return "tickets/list";
    }

    @GetMapping("/tickets/create")
    public String createTicketPage(Model model) {
        model.addAttribute("createTicketDto", new CreateTicketDto());
        model.addAttribute("priorities", TicketPriority.values());
        model.addAttribute("categories", TicketCategory.values());
        return "tickets/create";
    }

    @PostMapping("/tickets/create")
    public String submitTicket(@Valid @ModelAttribute("createTicketDto") CreateTicketDto dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserPrincipal principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("priorities", TicketPriority.values());
            model.addAttribute("categories", TicketCategory.values());
            return "tickets/create";
        }

        try {
            TicketResponseDto created = ticketService.createTicket(dto, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Ticket " + created.getTicketNumber() + " created successfully!");
            return "redirect:/tickets/" + created.getId();
        } catch (HelpDeskException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("priorities", TicketPriority.values());
            model.addAttribute("categories", TicketCategory.values());
            return "tickets/create";
        }
    }

    @GetMapping("/tickets/{id}")
    public String ticketDetails(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                Model model) {
        Ticket ticket = ticketService.getTicketEntityAndVerifyTenant(id, principal);
        var comments = commentService.getCommentsForTicket(id, principal);
        var attachments = attachmentService.getAttachmentsForTicket(id, principal);
        var history = statusHistoryRepository.findByTicketIdOrderByCreatedAtAsc(id);

        // Eligible status transitions
        List<TicketStatus> validNextStatuses = Arrays.stream(TicketStatus.values())
                .filter(s -> ticket.getStatus().canTransitionTo(s))
                .collect(Collectors.toList());

        // Organization agents for assignment dropdown (if staff)
        List<User> orgAgents = new ArrayList<>();
        if (principal.isStaff() && ticket.getOrganization() != null) {
            orgAgents = userRepository.findByOrganizationIdAndRole(ticket.getOrganization().getId(), Role.ROLE_AGENT);
        }

        model.addAttribute("ticket", ticketService.toDto(ticket));
        model.addAttribute("comments", comments);
        model.addAttribute("attachments", attachments);
        model.addAttribute("history", history);
        model.addAttribute("validNextStatuses", validNextStatuses);
        model.addAttribute("orgAgents", orgAgents);
        model.addAttribute("commentDto", new CreateCommentDto());
        model.addAttribute("isStaff", principal.isStaff());

        return "tickets/detail";
    }

    @PostMapping("/tickets/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam TicketStatus targetStatus,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal CustomUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            ticketService.transitionStatus(id, targetStatus, reason != null ? reason : "Status updated", principal);
            redirectAttributes.addFlashAttribute("successMessage", "Ticket status transitioned to " + targetStatus);
        } catch (HelpDeskException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                               @RequestParam Long agentId,
                               @AuthenticationPrincipal CustomUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            ticketService.assignTicket(id, agentId, principal);
            redirectAttributes.addFlashAttribute("successMessage", "Ticket assigned successfully.");
        } catch (HelpDeskException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentDto") CreateCommentDto commentDto,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal CustomUserPrincipal principal,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                commentService.addComment(id, commentDto, principal);
                redirectAttributes.addFlashAttribute("successMessage", commentDto.isInternalNote() ? "Internal note posted." : "Comment published.");
            } catch (HelpDeskException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            }
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/attachments")
    public String uploadAttachment(@PathVariable Long id,
                                   @RequestParam("file") MultipartFile file,
                                   @AuthenticationPrincipal CustomUserPrincipal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.uploadAttachment(id, null, file, principal);
            redirectAttributes.addFlashAttribute("successMessage", "File attached successfully.");
        } catch (HelpDeskException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id,
                                                       @AuthenticationPrincipal CustomUserPrincipal principal) {
        Resource resource = attachmentService.loadAttachmentAsResource(id, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
