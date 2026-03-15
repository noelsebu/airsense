-- V1: Core schema for AirSense

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE locations (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    floors INT DEFAULT 1,
    building_type VARCHAR(50) DEFAULT 'OTHER',
    ventilation_type VARCHAR(50) DEFAULT 'UNKNOWN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_location_account ON locations(account_id);

CREATE TABLE devices (
    id UUID PRIMARY KEY,
    serial_number VARCHAR(50) NOT NULL UNIQUE,
    device_type VARCHAR(50) NOT NULL,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    room_name VARCHAR(255),
    floor INT DEFAULT 1,
    firmware_version VARCHAR(50) DEFAULT '1.0.0',
    battery_level INT DEFAULT 100,
    last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_location ON devices(location_id);
CREATE INDEX idx_device_serial ON devices(serial_number);

CREATE TABLE sensor_samples (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    radon DOUBLE PRECISION,
    co2 DOUBLE PRECISION,
    voc DOUBLE PRECISION,
    pm25 DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    pressure DOUBLE PRECISION,
    radon_rating VARCHAR(10),
    co2_rating VARCHAR(10),
    voc_rating VARCHAR(10),
    pm25_rating VARCHAR(10),
    temperature_rating VARCHAR(10),
    humidity_rating VARCHAR(10),
    pressure_rating VARCHAR(10)
);

CREATE INDEX idx_sample_device_recorded ON sensor_samples(device_id, recorded_at);
CREATE INDEX idx_sample_recorded ON sensor_samples(recorded_at);

CREATE TABLE alert_rules (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    sensor_type VARCHAR(50) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rule_device ON alert_rules(device_id);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    alert_rule_id UUID NOT NULL REFERENCES alert_rules(id) ON DELETE CASCADE,
    sample_id UUID NOT NULL REFERENCES sensor_samples(id) ON DELETE CASCADE,
    triggered_value DOUBLE PRECISION NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    acknowledged BOOLEAN DEFAULT FALSE,
    triggered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rule ON alerts(alert_rule_id);
CREATE INDEX idx_alert_triggered ON alerts(triggered_at);
