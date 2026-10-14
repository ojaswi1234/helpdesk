package com.helpdesk.dto;

import java.time.Instant;

public class CommentResponseDto {

    private Long id;
    private Long ticketId;
    private Long userId;
    private String username;
    private String userFullName;
    private String userRole;
    private String content;
    private boolean internalNote;
    private Instant createdAt;

    public CommentResponseDto() {
    }

    public CommentResponseDto(Long id, Long ticketId, Long userId, String username, String userFullName, String userRole, String content, boolean internalNote, Instant createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.userId = userId;
        this.username = username;
        this.userFullName = userFullName;
        this.userRole = userRole;
        this.content = content;
        this.internalNote = internalNote;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
