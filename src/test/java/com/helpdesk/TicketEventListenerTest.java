package com.helpdesk;

import com.helpdesk.dto.WebSocketTicketEventDto;
import com.helpdesk.event.TicketEvent;
import com.helpdesk.event.TicketEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TicketEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TicketEventListener listener;

    @Test
    @DisplayName("Should publish STOMP updates to tenant-scoped topic, ticket topic, and customer queue")
    void testHandleTicketEvent_PublishesScopedDestinations() {
        TicketEvent event = new TicketEvent(
                "STATUS_CHANGED",
                101L,
                "HD-ACME-1001",
                1L, // orgId
                "Status Changed",
                "Ticket marked RESOLVED",
                "RESOLVED",
                "acme_customer",
                "acme_agent",
                "acme_agent"
        );

        listener.handleTicketEvent(event);

        // Verify tenant topic
        ArgumentCaptor<WebSocketTicketEventDto> tenantCaptor = ArgumentCaptor.forClass(WebSocketTicketEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/org/1/tickets"), tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getTicketNumber()).isEqualTo("HD-ACME-1001");
        assertThat(tenantCaptor.getValue().getStatus()).isEqualTo("RESOLVED");

        // Verify ticket topic
        ArgumentCaptor<WebSocketTicketEventDto> ticketCaptor = ArgumentCaptor.forClass(WebSocketTicketEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/tickets/101"), ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTicketId()).isEqualTo(101L);

        // Verify customer queue
        ArgumentCaptor<WebSocketTicketEventDto> userCaptor = ArgumentCaptor.forClass(WebSocketTicketEventDto.class);
        verify(messagingTemplate).convertAndSendToUser(eq("acme_customer"), eq("/queue/notifications"), userCaptor.capture());
        assertThat(userCaptor.getValue().getEventType()).isEqualTo("STATUS_CHANGED");
    }
}
