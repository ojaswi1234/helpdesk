package com.helpdesk.dto;

import jakarta.validation.constraints.NotNull;

public class AssignTicketDto {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    public AssignTicketDto() {
    }

    public AssignTicketDto(Long agentId) {
        this.agentId = agentId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
