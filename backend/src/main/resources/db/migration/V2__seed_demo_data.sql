-- =============================================================================
-- V2__seed_demo_data.sql
-- Demo/development seed data
-- First tenant: BarberÃ­a Demo
-- This is development data only â€” never hardcode business logic around this
-- =============================================================================

-- =============================================================================
-- BUSINESS: BarberÃ­a Demo (first tenant)
-- =============================================================================

INSERT INTO businesses (id, name, slug, business_type, description, address, phone, email, timezone, active)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'BarberÃ­a Demo',
    'barberia-demo',
    'BARBERSHOP',
    'BarberÃ­a clÃ¡sica con los mejores cortes de la ciudad. Atendemos con cita previa.',
    'Av. Principal 123, Lima, PerÃº',
    '+51999000001',
    'info@barberia-demo.com',
    'America/Lima',
    TRUE
);

-- =============================================================================
-- USER: Owner
-- Password: Demo1234! (bcrypt hash)
-- =============================================================================

INSERT INTO users (id, email, password_hash, first_name, last_name, phone, email_verified, active)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'owner@barberia-demo.com',
    '$2a$12$K8D4Ol5.EjnZXWYd4AkLvO9rRh8w9W3t3wLq1K5fO2Mv1cH0H.Tue',
    'Carlos',
    'Mendoza',
    '+51999000001',
    TRUE,
    TRUE
);

-- =============================================================================
-- BUSINESS_USER: Owner â†’ BarberÃ­a Demo
-- =============================================================================

INSERT INTO business_users (business_id, user_id, role, active)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'OWNER',
    TRUE
);

-- =============================================================================
-- BUSINESS CONFIGURATION
-- =============================================================================

INSERT INTO business_configurations (
    business_id,
    ai_assistant_name,
    ai_tone,
    ai_custom_rules,
    ai_faqs,
    ai_escalation_triggers,
    notification_emails,
    weekly_report_enabled,
    booking_lead_time_minutes,
    booking_max_days_ahead
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Nacho',
    'FRIENDLY',
    'Siempre saluda al cliente por su nombre. No hagas reservas fuera del horario de atenciÃ³n. Si el cliente pregunta por descuentos, indica que no tenemos descuentos disponibles actualmente y ofrece registrar su consulta.',
    '[
        {"question": "Â¿CuÃ¡nto cuesta un corte?", "answer": "El precio depende del servicio. Te puedo mostrar nuestra lista de servicios."},
        {"question": "Â¿DÃ³nde estÃ¡n ubicados?", "answer": "Estamos en Av. Principal 123, Lima."},
        {"question": "Â¿Atienden sin cita?", "answer": "Preferimos cita previa para garantizarte atenciÃ³n. Â¿Te ayudo a reservar?"}
    ]',
    ARRAY['reclamo', 'problema', 'devolucion', 'queja', 'insatisfecho', 'mal servicio', 'refund'],
    ARRAY['owner@barberia-demo.com'],
    TRUE,
    60,
    14
);

-- =============================================================================
-- BUSINESS HOURS (Monâ€“Sat, closed Sunday)
-- =============================================================================

INSERT INTO business_hours (business_id, day_of_week, open_time, close_time, is_closed)
VALUES
    ('11111111-1111-1111-1111-111111111111', 0, NULL, NULL, TRUE),    -- Sunday
    ('11111111-1111-1111-1111-111111111111', 1, '09:00', '19:00', FALSE),  -- Monday
    ('11111111-1111-1111-1111-111111111111', 2, '09:00', '19:00', FALSE),  -- Tuesday
    ('11111111-1111-1111-1111-111111111111', 3, '09:00', '19:00', FALSE),  -- Wednesday
    ('11111111-1111-1111-1111-111111111111', 4, '09:00', '19:00', FALSE),  -- Thursday
    ('11111111-1111-1111-1111-111111111111', 5, '09:00', '19:00', FALSE),  -- Friday
    ('11111111-1111-1111-1111-111111111111', 6, '09:00', '17:00', FALSE);  -- Saturday

-- =============================================================================
-- EMPLOYEES
-- =============================================================================

INSERT INTO employees (id, business_id, name, phone, email, active)
VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccc01', '11111111-1111-1111-1111-111111111111', 'Miguel Torres', '+51999000010', 'miguel@barberia-demo.com', TRUE),
    ('cccccccc-cccc-cccc-cccc-cccccccccc02', '11111111-1111-1111-1111-111111111111', 'Rodrigo DÃ­az', '+51999000011', 'rodrigo@barberia-demo.com', TRUE),
    ('cccccccc-cccc-cccc-cccc-cccccccccc03', '11111111-1111-1111-1111-111111111111', 'Luis GarcÃ­a', '+51999000012', 'luis@barberia-demo.com', TRUE);

-- =============================================================================
-- SERVICES
-- =============================================================================

INSERT INTO services (id, business_id, name, description, price, duration_minutes, active)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddd01', '11111111-1111-1111-1111-111111111111', 'Corte clÃ¡sico', 'Corte de cabello clÃ¡sico con tijera o mÃ¡quina.', 25.00, 30, TRUE),
    ('dddddddd-dddd-dddd-dddd-dddddddddd02', '11111111-1111-1111-1111-111111111111', 'Corte + barba', 'Corte de cabello y arreglo de barba.', 40.00, 45, TRUE),
    ('dddddddd-dddd-dddd-dddd-dddddddddd03', '11111111-1111-1111-1111-111111111111', 'Arreglo de barba', 'Perfilado y arreglo de barba con navaja.', 20.00, 20, TRUE),
    ('dddddddd-dddd-dddd-dddd-dddddddddd04', '11111111-1111-1111-1111-111111111111', 'Corte infantil', 'Corte de cabello para niÃ±os hasta 12 aÃ±os.', 18.00, 25, TRUE),
    ('dddddddd-dddd-dddd-dddd-dddddddddd05', '11111111-1111-1111-1111-111111111111', 'Tratamiento capilar', 'HidrataciÃ³n y tratamiento para el cabello.', 35.00, 40, TRUE);

-- =============================================================================
-- CUSTOMERS (Demo clients)
-- =============================================================================

INSERT INTO customers (id, business_id, phone, name, email, first_contact_at, last_contact_at)
VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', '11111111-1111-1111-1111-111111111111', '+51987654321', 'Juan PÃ©rez', 'juan@email.com', NOW() - INTERVAL '30 days', NOW() - INTERVAL '2 days'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', '11111111-1111-1111-1111-111111111111', '+51987654322', 'MarÃ­a GarcÃ­a', NULL, NOW() - INTERVAL '15 days', NOW() - INTERVAL '1 day'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', '11111111-1111-1111-1111-111111111111', '+51987654323', 'Pedro LÃ³pez', NULL, NOW() - INTERVAL '7 days', NOW()),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', '11111111-1111-1111-1111-111111111111', '+51987654324', 'Ana MartÃ­nez', 'ana@email.com', NOW() - INTERVAL '45 days', NOW() - INTERVAL '5 days'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', '11111111-1111-1111-1111-111111111111', '+51987654325', 'Roberto Silva', NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

-- =============================================================================
-- APPOINTMENTS (Demo appointments)
-- =============================================================================

INSERT INTO appointments (business_id, customer_id, service_id, employee_id, scheduled_at, duration_minutes, status, channel, created_by)
VALUES
    -- Confirmed upcoming
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'cccccccc-cccc-cccc-cccc-cccccccccc01', NOW() + INTERVAL '1 day' + INTERVAL '10 hours', 30, 'CONFIRMED', 'WHATSAPP', 'AI'),
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'cccccccc-cccc-cccc-cccc-cccccccccc02', NOW() + INTERVAL '1 day' + INTERVAL '14 hours', 45, 'CONFIRMED', 'WHATSAPP', 'AI'),
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 'dddddddd-dddd-dddd-dddd-dddddddddd01', 'cccccccc-cccc-cccc-cccc-cccccccccc01', NOW() + INTERVAL '2 days' + INTERVAL '11 hours', 30, 'PENDING', 'WHATSAPP', 'AI'),
    -- Past completed
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 'dddddddd-dddd-dddd-dddd-dddddddddd03', 'cccccccc-cccc-cccc-cccc-cccccccccc03', NOW() - INTERVAL '2 days' + INTERVAL '15 hours', 20, 'COMPLETED', 'WHATSAPP', 'AI'),
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'dddddddd-dddd-dddd-dddd-dddddddddd02', 'cccccccc-cccc-cccc-cccc-cccccccccc01', NOW() - INTERVAL '5 days' + INTERVAL '10 hours', 45, 'COMPLETED', 'WHATSAPP', 'AI'),
    -- Cancelled
    ('11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 'dddddddd-dddd-dddd-dddd-dddddddddd01', NULL, NOW() - INTERVAL '1 day' + INTERVAL '16 hours', 30, 'CANCELLED', 'WHATSAPP', 'AI');

-- =============================================================================
-- CONVERSATIONS (Demo conversations)
-- =============================================================================

INSERT INTO conversations (id, business_id, customer_id, status, last_message_at)
VALUES
    ('cccccccc-cccc-4ccc-accc-cccccccccc01', '11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'AI_ACTIVE', NOW() - INTERVAL '10 minutes'),
    ('cccccccc-cccc-4ccc-accc-cccccccccc02', '11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 'WAITING_HUMAN', NOW() - INTERVAL '5 minutes'),
    ('cccccccc-cccc-4ccc-accc-cccccccccc03', '11111111-1111-1111-1111-111111111111', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 'AI_ACTIVE', NOW() - INTERVAL '30 minutes');

-- Demo messages for conversation 1
INSERT INTO messages (conversation_id, business_id, direction, sender_type, content, status)
VALUES
    ('cccccccc-cccc-4ccc-accc-cccccccccc01', '11111111-1111-1111-1111-111111111111', 'INBOUND', 'CUSTOMER', 'Hola, Â¿quÃ© horarios tienen maÃ±ana?', 'READ'),
    ('cccccccc-cccc-4ccc-accc-cccccccccc01', '11111111-1111-1111-1111-111111111111', 'OUTBOUND', 'AI', 'Hola Juan! MaÃ±ana tenemos disponibilidad a las 10:00, 11:30, 15:00 y 17:00. Â¿Te interesa alguno?', 'READ');

-- Demo messages for conversation 2 (escalated)
INSERT INTO messages (conversation_id, business_id, direction, sender_type, content, status)
VALUES
    ('cccccccc-cccc-4ccc-accc-cccccccccc02', '11111111-1111-1111-1111-111111111111', 'INBOUND', 'CUSTOMER', 'Tengo un problema con mi reserva de ayer, quedÃ© muy insatisfecha con el servicio.', 'READ'),
    ('cccccccc-cccc-4ccc-accc-cccccccccc02', '11111111-1111-1111-1111-111111111111', 'OUTBOUND', 'AI', 'Entiendo tu molestia, MarÃ­a. Voy a transferirte con un miembro de nuestro equipo para que te atiendan personalmente.', 'READ');

-- Update escalation details for conversation 2
UPDATE conversations
SET status = 'WAITING_HUMAN', escalation_reason = 'Cliente insatisfecha â€” requiere atenciÃ³n personalizada', escalated_at = NOW() - INTERVAL '5 minutes'
WHERE id = 'cccccccc-cccc-4ccc-accc-cccccccccc02';

-- Demo messages for conversation 3
INSERT INTO messages (conversation_id, business_id, direction, sender_type, content, status)
VALUES
    ('cccccccc-cccc-4ccc-accc-cccccccccc03', '11111111-1111-1111-1111-111111111111', 'INBOUND', 'CUSTOMER', 'Quiero reservar para el viernes por la tarde', 'READ'),
    ('cccccccc-cccc-4ccc-accc-cccccccccc03', '11111111-1111-1111-1111-111111111111', 'OUTBOUND', 'AI', 'Claro Pedro! El viernes tenemos disponibilidad a las 14:00, 15:30 y 17:00. Â¿CuÃ¡l te viene mejor?', 'SENT');

