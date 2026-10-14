package com.helpdesk.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 40)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "first_response_deadline", nullable = false)
    private Instant firstResponseDeadline;

    @Column(name = "resolution_deadline", nullable = false)
    private Instant resolutionDeadline;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "first_response_breached", nullable = false)
    private boolean firstResponseBreached = false;

    @Column(name = "resolution_breached", nullable = false)
    private boolean resolutionBreached = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Ticket() {
    }

    public Ticket(String ticketNumber, Organization organization, User customer, String title,
                  String description, TicketPriority priority, TicketCategory category,
                  Instant firstResponseDeadline, Instant resolutionDeadline) {
        this.ticketNumber = ticketNumber;
        this.organization = organization;
        this.customer = customer;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.status = TicketStatus.OPEN;
        this.firstResponseDeadline = firstResponseDeadline;
        this.resolutionDeadline = resolutionDeadline;
        this.firstResponseBreached = false;
        this.resolutionBreached = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public User getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(User assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Instant getFirstResponseDeadline() {
        return firstResponseDeadline;
    }

    public void setFirstResponseDeadline(Instant firstResponseDeadline) {
        this.firstResponseDeadline = firstResponseDeadline;
    }

    public Instant getResolutionDeadline() {
        return resolutionDeadline;
    }

    public void setResolutionDeadline(Instant resolutionDeadline) {
        this.resolutionDeadline = resolutionDeadline;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public void setFirstRespondedAt(Instant firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean isFirstResponseBreached() {
        return firstResponseBreached;
    }

    public void setFirstResponseBreached(boolean firstResponseBreached) {
        this.firstResponseBreached = firstResponseBreached;
    }

    public boolean isResolutionBreached() {
        return resolutionBreached;
    }

    public void setResolutionBreached(boolean resolutionBreached) {
        this.resolutionBreached = resolutionBreached;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
