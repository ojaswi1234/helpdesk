package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCommentDto {

    @NotBlank(message = "Comment content cannot be blank")
    private String content;

    private boolean internalNote = false;

    public CreateCommentDto() {
    }

    public CreateCommentDto(String content, boolean internalNote) {
        this.content = content;
        this.internalNote = internalNote;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isInternalNote() {
        return internalNote;
    }

    public void setInternalNote(boolean internalNote) {
        this.internalNote = internalNote;
    }
}
