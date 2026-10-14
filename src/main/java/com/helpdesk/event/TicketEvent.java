package com.helpdesk.event;

import java.time.Instant;

public class TicketEvent {

    private final String eventType; // TICKET_CREATED, TICKET_ASSIGNED, STATUS_CHANGED, NEW_COMMENT, SLA_BREACH
    private final Long ticketId;
    private final String ticketNumber;
    private final Long organizationId;
    private final String title;
    private final String message;
    private final String status;
    private final String customerUsername;
    private final String assignedAgentUsername;
    private final String actorUsername;
    private final Instant timestamp;

    public TicketEvent(String eventType, Long ticketId, String ticketNumber, Long organizationId,
                       String title, String message, String status, String customerUsername,
                       String assignedAgentUsername, String actorUsername) {
        this.eventType = eventType;
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.organizationId = organizationId;
        this.title = title;
        this.message = message;
        this.status = status;
        this.customerUsername = customerUsername;
        this.assignedAgentUsername = assignedAgentUsername;
        this.actorUsername = actorUsername;
        this.timestamp = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public String getAssignedAgentUsername() {
        return assignedAgentUsername;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
