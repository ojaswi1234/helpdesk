package com.helpdesk.dto;

import com.helpdesk.model.TicketCategory;
import com.helpdesk.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTicketDto {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must provide at least 10 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private TicketPriority priority = TicketPriority.MEDIUM;

    @NotNull(message = "Category is required")
    private TicketCategory category = TicketCategory.TECHNICAL;

    public CreateTicketDto() {
    }

    public CreateTicketDto(String title, String description, TicketPriority priority, TicketCategory category) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }
}
