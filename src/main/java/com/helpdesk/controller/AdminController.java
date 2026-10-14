package com.helpdesk.controller;

import com.helpdesk.model.Organization;
import com.helpdesk.model.SlaPolicy;
import com.helpdesk.repository.AuditEventRepository;
import com.helpdesk.repository.OrganizationRepository;
import com.helpdesk.repository.SlaPolicyRepository;
import com.helpdesk.service.CustomUserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORGANIZATION_ADMIN')")
public class AdminController {

    private final OrganizationRepository organizationRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final AuditEventRepository auditEventRepository;

    public AdminController(OrganizationRepository organizationRepository,
                           SlaPolicyRepository slaPolicyRepository,
                           AuditEventRepository auditEventRepository) {
        this.organizationRepository = organizationRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping({"", "/organization"})
    public String organizationSettings(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        Long orgId = principal.getOrganizationId();
        Organization org = orgId != null ? organizationRepository.findById(orgId).orElse(null) : null;
        List<SlaPolicy> slaPolicies = orgId != null ? slaPolicyRepository.findByOrganizationId(orgId) : List.of();

        model.addAttribute("organization", org);
        model.addAttribute("slaPolicies", slaPolicies);
        model.addAttribute("isPlatformAdmin", principal.isPlatformAdmin());
        if (principal.isPlatformAdmin()) {
            model.addAttribute("allOrganizations", organizationRepository.findAll());
        }

        return "admin/organization";
    }

    @GetMapping("/audit")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "15") int size,
                            @AuthenticationPrincipal CustomUserPrincipal principal,
                            Model model) {
        Long orgId = principal.getOrganizationId();
        var auditPage = orgId != null
                ? auditEventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, PageRequest.of(page, size))
                : auditEventRepository.findAll(PageRequest.of(page, size));

        model.addAttribute("auditPage", auditPage);
        model.addAttribute("currentPage", page);
        return "admin/audit";
    }
}
