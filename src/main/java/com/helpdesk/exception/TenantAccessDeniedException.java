package com.helpdesk.exception;

public class TenantAccessDeniedException extends HelpDeskException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
