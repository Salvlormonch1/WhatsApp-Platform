-- =============================================================================
-- V1__initial_schema.sql
-- Initial schema for the multi-tenant SaaS platform
-- All tenant tables include business_id for row-level isolation
-- =============================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- ENUMS
-- =============================================================================

CREATE TYPE business_type AS ENUM (
    'BARBERSHOP',
    'HAIR_SALON',
    'BEAUTY_SALON',
    'NAIL_SALON',
    'SPA',
    'OTHER'
);

CREATE TYPE user_role AS ENUM (
    'OWNER',
    'ADMIN',
    'EMPLOYEE'
);

CREATE TYPE ai_tone AS ENUM (
    'FORMAL',
    'FRIENDLY',
    'CASUAL'
);

CREATE TYPE appointment_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'CANCELLED',
    'COMPLETED',
    'NO_SHOW'
);

CREATE TYPE appointment_channel AS ENUM (
    'WHATSAPP',
    'MANUAL',
    'WEB'
);

CREATE TYPE appointment_created_by AS ENUM (
    'AI',
    'HUMAN'
);

CREATE TYPE conversation_status AS ENUM (
    'AI_ACTIVE',
    'WAITING_HUMAN',
    'HUMAN_ACTIVE',
    'RESOLVED'
);

CREATE TYPE message_direction AS ENUM (
    'INBOUND',
    'OUTBOUND'
);

CREATE TYPE message_sender_type AS ENUM (
    'CUSTOMER',
    'AI',
    'HUMAN'
);

CREATE TYPE message_status AS ENUM (
    'SENT',
    'DELIVERED',
    'READ',
    'FAILED'
);

CREATE TYPE report_type AS ENUM (
    'WEEKLY',
    'MONTHLY'
);

-- =============================================================================
-- BUSINESSES (Tenants)
-- =============================================================================

CREATE TABLE businesses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    business_type   business_type NOT NULL DEFAULT 'OTHER',
    description     TEXT,
    address         TEXT,
    phone           VARCHAR(50),
    email           VARCHAR(255),
    website         VARCHAR(500),
    social_links    JSONB DEFAULT '{}',
    timezone        VARCHAR(100) NOT NULL DEFAULT 'UTC',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_businesses_slug ON businesses(slug);
CREATE INDEX idx_businesses_active ON businesses(active);

-- =============================================================================
-- USERS (Platform accounts — can belong to multiple businesses)
-- =============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(50),
    avatar_url      VARCHAR(500),
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);

-- =============================================================================
-- BUSINESS_USERS (User ↔ Business membership with role)
-- =============================================================================

CREATE TABLE business_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            user_role NOT NULL DEFAULT 'EMPLOYEE',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, user_id)
);

CREATE INDEX idx_business_users_business ON business_users(business_id);
CREATE INDEX idx_business_users_user ON business_users(user_id);

-- =============================================================================
-- REFRESH_TOKENS
-- =============================================================================

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token_hash);

-- =============================================================================
-- BUSINESS_CONFIGURATIONS
-- =============================================================================

CREATE TABLE business_configurations (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id                 UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE UNIQUE,
    ai_assistant_name           VARCHAR(100) DEFAULT 'Asistente',
    ai_tone                     ai_tone NOT NULL DEFAULT 'FRIENDLY',
    ai_custom_rules             TEXT,
    ai_faqs                     JSONB DEFAULT '[]',
    ai_escalation_triggers      TEXT[] DEFAULT '{}',
    notification_emails         TEXT[] DEFAULT '{}',
    weekly_report_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    booking_lead_time_minutes   INT NOT NULL DEFAULT 60,
    booking_max_days_ahead      INT NOT NULL DEFAULT 30,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- BUSINESS_HOURS
-- =============================================================================

CREATE TABLE business_hours (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    day_of_week     SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    open_time       TIME,
    close_time      TIME,
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(business_id, day_of_week)
);

CREATE INDEX idx_business_hours_business ON business_hours(business_id);

-- =============================================================================
-- EMPLOYEES
-- =============================================================================

CREATE TABLE employees (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    email           VARCHAR(255),
    bio             TEXT,
    avatar_url      VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_business ON employees(business_id);
CREATE INDEX idx_employees_active ON employees(business_id, active);

-- =============================================================================
-- SERVICES (What the business offers)
-- =============================================================================

CREATE TABLE services (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    price               NUMERIC(10,2) NOT NULL DEFAULT 0,
    duration_minutes    INT NOT NULL DEFAULT 30,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_services_business ON services(business_id);
CREATE INDEX idx_services_active ON services(business_id, active);

-- =============================================================================
-- CUSTOMERS (Per-tenant: same phone can exist in multiple businesses)
-- =============================================================================

CREATE TABLE customers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    phone               VARCHAR(50) NOT NULL,
    name                VARCHAR(255),
    email               VARCHAR(255),
    notes               TEXT,
    first_contact_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_contact_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, phone)
);

CREATE INDEX idx_customers_business ON customers(business_id);
CREATE INDEX idx_customers_phone ON customers(business_id, phone);

-- =============================================================================
-- APPOINTMENTS
-- =============================================================================

CREATE TABLE appointments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id             UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    customer_id             UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    service_id              UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    employee_id             UUID REFERENCES employees(id) ON DELETE SET NULL,
    scheduled_at            TIMESTAMPTZ NOT NULL,
    duration_minutes        INT NOT NULL,
    status                  appointment_status NOT NULL DEFAULT 'PENDING',
    notes                   TEXT,
    channel                 appointment_channel NOT NULL DEFAULT 'WHATSAPP',
    created_by              appointment_created_by NOT NULL DEFAULT 'AI',
    cancelled_reason        TEXT,
    confirmation_sent_at    TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_business ON appointments(business_id);
CREATE INDEX idx_appointments_customer ON appointments(business_id, customer_id);
CREATE INDEX idx_appointments_scheduled ON appointments(business_id, scheduled_at);
CREATE INDEX idx_appointments_status ON appointments(business_id, status);
CREATE INDEX idx_appointments_employee ON appointments(business_id, employee_id);

-- =============================================================================
-- WHATSAPP_INTEGRATIONS
-- =============================================================================

CREATE TABLE whatsapp_integrations (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id                     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE UNIQUE,
    phone_number_id                 VARCHAR(100) NOT NULL UNIQUE,
    whatsapp_business_account_id    VARCHAR(100),
    access_token_encrypted          TEXT NOT NULL,
    verify_token                    VARCHAR(255),
    display_phone                   VARCHAR(50),
    active                          BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_whatsapp_phone_number_id ON whatsapp_integrations(phone_number_id);

-- =============================================================================
-- CONVERSATIONS
-- =============================================================================

CREATE TABLE conversations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    customer_id         UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    status              conversation_status NOT NULL DEFAULT 'AI_ACTIVE',
    assigned_to         UUID REFERENCES users(id) ON DELETE SET NULL,
    channel             VARCHAR(50) NOT NULL DEFAULT 'WHATSAPP',
    escalation_reason   TEXT,
    escalated_at        TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    last_message_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_business ON conversations(business_id);
CREATE INDEX idx_conversations_customer ON conversations(business_id, customer_id);
CREATE INDEX idx_conversations_status ON conversations(business_id, status);
CREATE INDEX idx_conversations_last_message ON conversations(business_id, last_message_at DESC);

-- =============================================================================
-- MESSAGES
-- =============================================================================

CREATE TABLE messages (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id         UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    business_id             UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    direction               message_direction NOT NULL,
    sender_type             message_sender_type NOT NULL,
    content                 TEXT NOT NULL,
    whatsapp_message_id     VARCHAR(255),
    status                  message_status NOT NULL DEFAULT 'SENT',
    metadata                JSONB DEFAULT '{}',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_business ON messages(business_id);
CREATE INDEX idx_messages_whatsapp_id ON messages(whatsapp_message_id) WHERE whatsapp_message_id IS NOT NULL;

-- =============================================================================
-- AI_INTERACTIONS (Observability)
-- =============================================================================

CREATE TABLE ai_interactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id     UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    message_id          UUID REFERENCES messages(id) ON DELETE SET NULL,
    tool_name           VARCHAR(100),
    tool_input          JSONB,
    tool_output         JSONB,
    prompt_tokens       INT,
    completion_tokens   INT,
    duration_ms         INT,
    success             BOOLEAN NOT NULL DEFAULT TRUE,
    error               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_interactions_conversation ON ai_interactions(conversation_id);
CREATE INDEX idx_ai_interactions_business ON ai_interactions(business_id);

-- =============================================================================
-- REPORTS
-- =============================================================================

CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    report_type     report_type NOT NULL DEFAULT 'WEEKLY',
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    data            JSONB NOT NULL DEFAULT '{}',
    sent_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_business ON reports(business_id);
CREATE INDEX idx_reports_period ON reports(business_id, period_start DESC);

-- =============================================================================
-- AUDIT_LOGS
-- =============================================================================

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID REFERENCES businesses(id) ON DELETE SET NULL,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       UUID,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_business ON audit_logs(business_id, created_at DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at DESC);

-- =============================================================================
-- AUTO-UPDATE updated_at trigger function
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
CREATE TRIGGER trg_businesses_updated_at BEFORE UPDATE ON businesses FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_business_configurations_updated_at BEFORE UPDATE ON business_configurations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_employees_updated_at BEFORE UPDATE ON employees FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_services_updated_at BEFORE UPDATE ON services FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_appointments_updated_at BEFORE UPDATE ON appointments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_whatsapp_integrations_updated_at BEFORE UPDATE ON whatsapp_integrations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_conversations_updated_at BEFORE UPDATE ON conversations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
