package com.helpdesk.exception;

public class InvalidStatusTransitionException extends HelpDeskException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
