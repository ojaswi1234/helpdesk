package com.helpdesk.event;

import com.helpdesk.dto.WebSocketTicketEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class TicketEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    public TicketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketEvent(TicketEvent event) {
        log.info("Dispatching real-time WebSocket events for ticket: {} (event: {})", event.getTicketNumber(), event.getEventType());

        WebSocketTicketEventDto payload = new WebSocketTicketEventDto(
                UUID.randomUUID().toString(),
                event.getEventType(),
                event.getTicketId(),
                event.getTicketNumber(),
                event.getOrganizationId(),
                event.getTitle(),
                event.getMessage(),
                event.getStatus(),
                event.getActorUsername(),
                event.getTimestamp()
        );

        // 1. Tenant-scoped event for organization dashboard
        if (event.getOrganizationId() != null) {
            messagingTemplate.convertAndSend("/topic/org/" + event.getOrganizationId() + "/tickets", payload);
        }

        // 2. Ticket-scoped destination for ticket conversation view
        messagingTemplate.convertAndSend("/topic/tickets/" + event.getTicketId(), payload);

        // 3. User-specific private notification for ticket creator (customer)
        if (event.getCustomerUsername() != null) {
            messagingTemplate.convertAndSendToUser(
                    event.getCustomerUsername(),
                    "/queue/notifications",
                    payload
            );
        }

        // 4. User-specific private notification for assigned agent
        if (event.getAssignedAgentUsername() != null && !event.getAssignedAgentUsername().equals(event.getActorUsername())) {
            messagingTemplate.convertAndSendToUser(
                    event.getAssignedAgentUsername(),
                    "/queue/notifications",
                    payload
            );
        }

        log.debug("WebSocket ticket events dispatched for {}", event.getTicketNumber());
    }
}
