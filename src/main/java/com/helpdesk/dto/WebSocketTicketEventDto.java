package com.helpdesk.dto;

import java.time.Instant;

public class WebSocketTicketEventDto {

    private String eventId;
    private String eventType; // TICKET_CREATED, TICKET_ASSIGNED, STATUS_CHANGED, NEW_COMMENT, SLA_BREACH
    private Long ticketId;
    private String ticketNumber;
    private Long organizationId;
    private String title;
    private String message;
    private String status;
    private String updatedBy;
    private Instant timestamp;

    public WebSocketTicketEventDto() {
    }

    public WebSocketTicketEventDto(String eventId, String eventType, Long ticketId, String ticketNumber,
                                   Long organizationId, String title, String message, String status,
                                   String updatedBy, Instant timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.ticketId = ticketId;
        this.ticketNumber = ticketNumber;
        this.organizationId = organizationId;
        this.title = title;
        this.message = message;
        this.status = status;
        this.updatedBy = updatedBy;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
