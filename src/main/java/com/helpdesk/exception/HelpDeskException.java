package com.helpdesk.exception;

public class HelpDeskException extends RuntimeException {
    public HelpDeskException(String message) {
        super(message);
    }

    public HelpDeskException(String message, Throwable cause) {
        super(message, cause);
    }
}
