package com.helpdesk.controller;

import com.helpdesk.dto.TicketResponseDto;
import com.helpdesk.model.Role;
import com.helpdesk.model.TicketStatus;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.service.CustomUserPrincipal;
import com.helpdesk.service.NotificationService;
import com.helpdesk.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    public DashboardController(TicketService ticketService,
                               TicketRepository ticketRepository,
                               NotificationService notificationService) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long orgId = principal.getOrganizationId();

        long totalTickets = orgId != null ? ticketRepository.countByOrganizationId(orgId) : ticketRepository.count();
        long openTickets = orgId != null ? ticketRepository.countByOrganizationIdAndStatus(orgId, TicketStatus.OPEN) : ticketRepository.count();
        long breachedTickets = orgId != null ? ticketRepository.countByOrganizationIdAndResolutionBreachedTrue(orgId) : 0;

        Page<TicketResponseDto> recentTickets = ticketService.getTickets(
                principal,
                null,
                PageRequest.of(0, 8, Sort.by("createdAt").descending())
        );

        model.addAttribute("user", principal);
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("openTickets", openTickets);
        model.addAttribute("breachedTickets", breachedTickets);
        model.addAttribute("recentTickets", recentTickets.getContent());
        model.addAttribute("unreadNotifications", notificationService.getUnreadCount(principal.getUserId()));

        return "dashboard";
    }
}
