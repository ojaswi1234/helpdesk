package com.helpdesk.dto;

import com.helpdesk.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateTicketStatusDto {

    @NotNull(message = "Target status is required")
    private TicketStatus targetStatus;

    private String reason;

    public UpdateTicketStatusDto() {
    }

    public UpdateTicketStatusDto(TicketStatus targetStatus, String reason) {
        this.targetStatus = targetStatus;
        this.reason = reason;
    }

    public TicketStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(TicketStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
