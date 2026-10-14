-- V2__seed_demo_data.sql: HelpDesk Pro Demo Seed Data

-- 1. Demo Organizations
INSERT INTO organizations (id, name, slug, contact_email, created_at, updated_at)
VALUES
(1, 'Acme Corp', 'acme-corp', 'support@acme.internal', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Starlight Logistics', 'starlight-logistics', 'help@starlight.internal', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. SLA Policies for Acme Corp (Org 1)
INSERT INTO sla_policies (id, organization_id, priority, first_response_hours, resolution_hours, created_at)
VALUES
(1, 1, 'URGENT', 1, 4, CURRENT_TIMESTAMP),
(2, 1, 'HIGH', 2, 8, CURRENT_TIMESTAMP),
(3, 1, 'MEDIUM', 4, 24, CURRENT_TIMESTAMP),
(4, 1, 'LOW', 8, 48, CURRENT_TIMESTAMP),
(5, 2, 'URGENT', 1, 4, CURRENT_TIMESTAMP),
(6, 2, 'HIGH', 2, 8, CURRENT_TIMESTAMP),
(7, 2, 'MEDIUM', 4, 24, CURRENT_TIMESTAMP),
(8, 2, 'LOW', 8, 48, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 3. Demo Users
INSERT INTO users (id, username, email, password_hash, full_name, organization_id, role, enabled, created_at, updated_at)
VALUES
(1, 'superadmin', 'superadmin@helpdesk.platform', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Platform Super Admin', NULL, 'ROLE_PLATFORM_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'acme_admin', 'admin@acme.internal', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Acme Admin', 1, 'ROLE_ORGANIZATION_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'acme_agent', 'agent@acme.internal', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Sarah Jenkins (Agent)', 1, 'ROLE_AGENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'acme_customer', 'customer@client.com', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'David Miller', 1, 'ROLE_CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'star_admin', 'admin@starlight.internal', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Starlight Admin', 2, 'ROLE_ORGANIZATION_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'star_customer', 'ops@partner.com', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Emma Watson', 2, 'ROLE_CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 4. Demo Tickets in Acme Corp (Org 1)
INSERT INTO tickets (id, ticket_number, organization_id, customer_id, assigned_agent_id, title, description, priority, category, status, first_response_deadline, resolution_deadline, created_at, updated_at)
VALUES
(1, 'HD-ACME-1001', 1, 4, 3, 'Cannot access billing invoices portal', 'When clicking download on the latest monthly invoice, a 403 error is received.', 'HIGH', 'BILLING', 'ASSIGNED', CURRENT_TIMESTAMP + INTERVAL '2 hours', CURRENT_TIMESTAMP + INTERVAL '8 hours', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'HD-ACME-1002', 1, 4, NULL, 'SSO integration token expiration issue', 'Users are logged out every 15 minutes instead of the configured 8 hours session duration.', 'URGENT', 'ACCESS', 'OPEN', CURRENT_TIMESTAMP + INTERVAL '1 hour', CURRENT_TIMESTAMP + INTERVAL '4 hours', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 5. Demo Tickets in Starlight Logistics (Org 2) - Tenant Isolated
INSERT INTO tickets (id, ticket_number, organization_id, customer_id, assigned_agent_id, title, description, priority, category, status, first_response_deadline, resolution_deadline, created_at, updated_at)
VALUES
(3, 'HD-STAR-2001', 2, 6, NULL, 'Tracking webhook latency in EU region', 'Webhook events for container transit updates are delayed by 45 minutes.', 'MEDIUM', 'TECHNICAL', 'OPEN', CURRENT_TIMESTAMP + INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '24 hours', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 6. Demo Comments
INSERT INTO ticket_comments (id, ticket_id, user_id, content, is_internal_note, created_at)
VALUES
(1, 1, 4, 'I verified this occurs on both Chrome and Firefox browsers.', FALSE, CURRENT_TIMESTAMP),
(2, 1, 3, 'Internal note: checked backend S3 permissions for customer org 1, regenerating IAM role policy.', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Update sequences if PostgreSQL
SELECT setval('organizations_id_seq', (SELECT COALESCE(MAX(id), 1) FROM organizations));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('sla_policies_id_seq', (SELECT COALESCE(MAX(id), 1) FROM sla_policies));
SELECT setval('tickets_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tickets));
SELECT setval('ticket_comments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM ticket_comments));
