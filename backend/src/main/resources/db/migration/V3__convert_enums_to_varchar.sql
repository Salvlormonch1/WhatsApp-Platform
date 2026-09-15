-- =============================================================================
-- V3__convert_enums_to_varchar.sql
-- Convert all PostgreSQL native enum types to VARCHAR(50)
-- Reason: Hibernate @Enumerated(EnumType.STRING) sends VARCHAR, not native enum
-- Note: Native types are kept (not dropped) to avoid dependency issues
-- =============================================================================

-- businesses.business_type
ALTER TABLE businesses ALTER COLUMN business_type TYPE VARCHAR(50) USING business_type::VARCHAR;

-- business_users.role
ALTER TABLE business_users ALTER COLUMN role TYPE VARCHAR(50) USING role::VARCHAR;

-- business_configurations.ai_tone
ALTER TABLE business_configurations ALTER COLUMN ai_tone TYPE VARCHAR(50) USING ai_tone::VARCHAR;

-- appointments.status
ALTER TABLE appointments ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

-- appointments.channel
ALTER TABLE appointments ALTER COLUMN channel TYPE VARCHAR(50) USING channel::VARCHAR;

-- appointments.created_by
ALTER TABLE appointments ALTER COLUMN created_by TYPE VARCHAR(50) USING created_by::VARCHAR;

-- conversations.status
ALTER TABLE conversations ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

-- messages.direction
ALTER TABLE messages ALTER COLUMN direction TYPE VARCHAR(50) USING direction::VARCHAR;

-- messages.sender_type
ALTER TABLE messages ALTER COLUMN sender_type TYPE VARCHAR(50) USING sender_type::VARCHAR;

-- messages.status
ALTER TABLE messages ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

-- reports.report_type
ALTER TABLE reports ALTER COLUMN report_type TYPE VARCHAR(50) USING report_type::VARCHAR;
