package com.helpdesk.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> VALID_TRANSITIONS = Map.of(
            OPEN, EnumSet.of(ASSIGNED, CLOSED),
            ASSIGNED, EnumSet.of(IN_PROGRESS, OPEN, RESOLVED, CLOSED),
            IN_PROGRESS, EnumSet.of(RESOLVED, ASSIGNED, CLOSED),
            RESOLVED, EnumSet.of(CLOSED, IN_PROGRESS, OPEN),
            CLOSED, EnumSet.of(OPEN)
    );

    public boolean canTransitionTo(TicketStatus target) {
        if (target == null || target == this) {
            return false;
        }
        Set<TicketStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
