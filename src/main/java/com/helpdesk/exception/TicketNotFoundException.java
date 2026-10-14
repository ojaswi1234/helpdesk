package com.helpdesk.exception;

public class TicketNotFoundException extends HelpDeskException {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
