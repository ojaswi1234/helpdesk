/**
 * HelpDesk Pro Multi-Tenant Real-Time WebSocket Client
 * Connects securely to /ws-helpdesk and dynamically updates tickets and comments.
 */
document.addEventListener("DOMContentLoaded", function () {
    let stompClient = null;
    let reconnectAttempts = 0;
    const maxReconnectAttempts = 5;

    const orgId = document.body.getAttribute("data-org-id");
    const ticketId = document.body.getAttribute("data-ticket-id");

    function initWebSocket() {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn("SockJS or Stomp library not available. Real-time updates disabled.");
            return;
        }

        const socket = new SockJS('/ws-helpdesk');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function (frame) {
            reconnectAttempts = 0;
            console.log("Connected to HelpDesk Pro WebSocket broker");
            updateConnectionStatus(true);

            // 1. User-specific notifications
            stompClient.subscribe('/user/queue/notifications', function (message) {
                try {
                    const event = JSON.parse(message.body);
                    showToast(event.title || "Ticket Notification", event.message, "info");
                } catch (e) {
                    console.error("Error parsing user notification:", e);
                }
            });

            // 2. Tenant-scoped ticket updates
            if (orgId && orgId !== "null") {
                stompClient.subscribe('/topic/org/' + orgId + '/tickets', function (message) {
                    try {
                        const event = JSON.parse(message.body);
                        handleTenantTicketEvent(event);
                    } catch (e) {
                        console.error("Error parsing org ticket event:", e);
                    }
                });
            }

            // 3. Ticket-scoped live conversation updates
            if (ticketId && ticketId !== "null") {
                stompClient.subscribe('/topic/tickets/' + ticketId, function (message) {
                    try {
                        const event = JSON.parse(message.body);
                        handleTicketDetailEvent(event);
                    } catch (e) {
                        console.error("Error parsing ticket event:", e);
                    }
                });
            }

        }, function (error) {
            console.warn("WebSocket disconnected:", error);
            updateConnectionStatus(false);
            attemptReconnect();
        });
    }

    function attemptReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            const timeout = Math.min(1000 * Math.pow(2, reconnectAttempts), 15000);
            setTimeout(initWebSocket, timeout);
        }
    }

    function updateConnectionStatus(connected) {
        const indicator = document.getElementById("ws-status-indicator");
        if (indicator) {
            indicator.className = connected ? "badge bg-success" : "badge bg-warning";
            indicator.textContent = connected ? "Live (Connected)" : "Connecting...";
        }
    }

    function handleTenantTicketEvent(event) {
        showToast(event.title || "Ticket Update", event.message, "primary");
    }

    function handleTicketDetailEvent(event) {
        if (event.eventType === "STATUS_CHANGED") {
            const statusBadge = document.getElementById("ticket-status-badge");
            if (statusBadge) {
                statusBadge.textContent = event.status;
                statusBadge.className = "badge fs-6 badge-" + event.status.toLowerCase();
            }
            showToast("Status Changed", `Ticket status changed to ${event.status}`, "warning");
        } else if (event.eventType === "NEW_COMMENT") {
            showToast("New Comment", event.message, "success");
            // Reload comments or prompt
            const commentContainer = document.getElementById("live-comment-indicator");
            if (commentContainer) {
                commentContainer.classList.remove("d-none");
            }
        }
    }

    function showToast(title, message, type) {
        const container = document.getElementById("toast-container");
        if (!container) return;

        const toastId = "toast-" + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-bg-${type} border-0 show shadow-lg mb-2" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <strong>${title}</strong><br/>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;
        container.insertAdjacentHTML("beforeend", toastHtml);
        const toastElement = document.getElementById(toastId);
        setTimeout(() => {
            if (toastElement) {
                toastElement.remove();
            }
        }, 6000);
    }

    initWebSocket();
});
