package com.helpdesk.exception;

public class AttachmentStorageException extends HelpDeskException {
    public AttachmentStorageException(String message) {
        super(message);
    }

    public AttachmentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
