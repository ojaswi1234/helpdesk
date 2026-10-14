package com.helpdesk.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sla_policies", uniqueConstraints = {
        @UniqueConstraint(name = "uq_org_priority", columnNames = {"organization_id", "priority"})
})
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Column(name = "first_response_hours", nullable = false)
    private int firstResponseHours;

    @Column(name = "resolution_hours", nullable = false)
    private int resolutionHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SlaPolicy() {
    }

    public SlaPolicy(Organization organization, TicketPriority priority, int firstResponseHours, int resolutionHours) {
        this.organization = organization;
        this.priority = priority;
        this.firstResponseHours = firstResponseHours;
        this.resolutionHours = resolutionHours;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public int getFirstResponseHours() {
        return firstResponseHours;
    }

    public void setFirstResponseHours(int firstResponseHours) {
        this.firstResponseHours = firstResponseHours;
    }

    public int getResolutionHours() {
        return resolutionHours;
    }

    public void setResolutionHours(int resolutionHours) {
        this.resolutionHours = resolutionHours;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
