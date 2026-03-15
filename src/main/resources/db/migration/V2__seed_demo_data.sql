-- V2: Seed demo data

-- Demo account
INSERT INTO accounts (id, name) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Demo Account');

-- Locations
INSERT INTO locations (id, account_id, name, address, floors, building_type, ventilation_type) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
     'Stavanger HQ', 'Øvre Holmegate 22, 4006 Stavanger', 3, 'OFFICE', 'MECHANICAL'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
     'Oslo Lab', 'Wergelandsveien 7, 0167 Oslo', 2, 'OFFICE', 'HYBRID'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',
     'Home Office', 'Eiganesveien 1, 4009 Stavanger', 1, 'RESIDENTIAL', 'NATURAL');

-- Devices at Stavanger HQ
INSERT INTO devices (id, serial_number, device_type, location_id, room_name, floor, firmware_version) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'SN-2960-001', 'VIEW_PLUS',
     'b0000000-0000-0000-0000-000000000001', 'Open Office', 1, '3.5.4'),
    ('d0000000-0000-0000-0000-000000000002', 'SN-2930-001', 'WAVE_PLUS',
     'b0000000-0000-0000-0000-000000000001', 'Meeting Room A', 2, '3.2.1'),
    ('d0000000-0000-0000-0000-000000000003', 'SN-2920-001', 'WAVE_RADON',
     'b0000000-0000-0000-0000-000000000001', 'Basement Storage', 0, '2.8.0');

-- Devices at Oslo Lab
INSERT INTO devices (id, serial_number, device_type, location_id, room_name, floor, firmware_version) VALUES
    ('d0000000-0000-0000-0000-000000000004', 'SN-2960-002', 'VIEW_PLUS',
     'b0000000-0000-0000-0000-000000000002', 'Lab Floor', 1, '3.5.4'),
    ('d0000000-0000-0000-0000-000000000005', 'SN-3110-001', 'SPACE_PRO',
     'b0000000-0000-0000-0000-000000000002', 'Server Room', 2, '4.0.1');

-- Devices at Home Office
INSERT INTO devices (id, serial_number, device_type, location_id, room_name, floor) VALUES
    ('d0000000-0000-0000-0000-000000000006', 'SN-2950-001', 'WAVE_MINI',
     'b0000000-0000-0000-0000-000000000003', 'Living Room', 1),
    ('d0000000-0000-0000-0000-000000000007', 'SN-2920-002', 'WAVE_RADON',
     'b0000000-0000-0000-0000-000000000003', 'Bedroom', 1);

-- Default alert rules
INSERT INTO alert_rules (id, device_id, sensor_type, threshold) VALUES
    (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000001', 'CO2', 1000.0),
    (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000001', 'RADON', 150.0),
    (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000003', 'RADON', 100.0),
    (RANDOM_UUID(), 'd0000000-0000-0000-0000-000000000005', 'TEMPERATURE', 30.0);
