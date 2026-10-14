package com.helpdesk.model;

public enum TicketPriority {
    URGENT(1, 4),
    HIGH(2, 8),
    MEDIUM(4, 24),
    LOW(8, 48);

    private final int defaultFirstResponseHours;
    private final int defaultResolutionHours;

    TicketPriority(int defaultFirstResponseHours, int defaultResolutionHours) {
        this.defaultFirstResponseHours = defaultFirstResponseHours;
        this.defaultResolutionHours = defaultResolutionHours;
    }

    public int getDefaultFirstResponseHours() {
        return defaultFirstResponseHours;
    }

    public int getDefaultResolutionHours() {
        return defaultResolutionHours;
    }
}
