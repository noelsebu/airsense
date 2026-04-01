-- V3: Seed demo sensor samples and alerts for immediate dashboard visibility

-- Sensor samples (above-threshold values to trigger alerts)
INSERT INTO sensor_samples (id, device_id, recorded_at,
    co2, radon, voc, pm25, temperature, humidity, pressure,
    co2_rating, radon_rating, voc_rating, pm25_rating, temperature_rating, humidity_rating, pressure_rating)
VALUES
    -- Device 1 (Open Office): high CO2 + high Radon, 3 hours ago
    ('f0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001',
     DATEADD('HOUR', -3, CURRENT_TIMESTAMP),
     1285.0, 172.0, 320.0, 7.2, 22.5, 47.0, 1011.0,
     'FAIR', 'POOR', 'GOOD', 'GOOD', 'GOOD', 'GOOD', 'GOOD'),

    -- Device 3 (Basement Storage): high Radon, 5 hours ago
    ('f0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003',
     DATEADD('HOUR', -5, CURRENT_TIMESTAMP),
     NULL, 138.0, NULL, NULL, NULL, NULL, NULL,
     NULL, 'POOR', NULL, NULL, NULL, NULL, NULL),

    -- Device 5 (Server Room): high Temperature, 1 hour ago (acknowledged)
    ('f0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000005',
     DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
     850.0, NULL, 210.0, 4.1, 31.8, 38.0, 1013.0,
     'GOOD', NULL, 'GOOD', 'GOOD', 'FAIR', 'GOOD', 'GOOD');

-- Alerts referencing the seeded samples and looking up alert rule IDs by device + sensor type
-- Active: CO2 alert on device 1
INSERT INTO alerts (id, alert_rule_id, sample_id, triggered_value, threshold, acknowledged, triggered_at)
SELECT RANDOM_UUID(), ar.id, 'f0000000-0000-0000-0000-000000000001',
       1285.0, ar.threshold, false, DATEADD('HOUR', -3, CURRENT_TIMESTAMP)
FROM alert_rules ar
WHERE ar.device_id = 'd0000000-0000-0000-0000-000000000001' AND ar.sensor_type = 'CO2';

-- Active: Radon alert on device 3
INSERT INTO alerts (id, alert_rule_id, sample_id, triggered_value, threshold, acknowledged, triggered_at)
SELECT RANDOM_UUID(), ar.id, 'f0000000-0000-0000-0000-000000000002',
       138.0, ar.threshold, false, DATEADD('HOUR', -5, CURRENT_TIMESTAMP)
FROM alert_rules ar
WHERE ar.device_id = 'd0000000-0000-0000-0000-000000000003' AND ar.sensor_type = 'RADON';

-- Acknowledged: Radon alert on device 1
INSERT INTO alerts (id, alert_rule_id, sample_id, triggered_value, threshold, acknowledged, triggered_at)
SELECT RANDOM_UUID(), ar.id, 'f0000000-0000-0000-0000-000000000001',
       172.0, ar.threshold, true, DATEADD('HOUR', -3, CURRENT_TIMESTAMP)
FROM alert_rules ar
WHERE ar.device_id = 'd0000000-0000-0000-0000-000000000001' AND ar.sensor_type = 'RADON';

-- Acknowledged: Temperature alert on device 5
INSERT INTO alerts (id, alert_rule_id, sample_id, triggered_value, threshold, acknowledged, triggered_at)
SELECT RANDOM_UUID(), ar.id, 'f0000000-0000-0000-0000-000000000003',
       31.8, ar.threshold, true, DATEADD('HOUR', -1, CURRENT_TIMESTAMP)
FROM alert_rules ar
WHERE ar.device_id = 'd0000000-0000-0000-0000-000000000005' AND ar.sensor_type = 'TEMPERATURE';
