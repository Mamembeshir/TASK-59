INSERT INTO sync_config (sync_name, enabled, lan_only, cron_expression, endpoint)
VALUES ('LAN_OPTIONAL_SYNC', FALSE, TRUE, NULL, NULL)
ON DUPLICATE KEY UPDATE
enabled = VALUES(enabled),
lan_only = VALUES(lan_only),
cron_expression = VALUES(cron_expression),
endpoint = VALUES(endpoint);

CREATE INDEX idx_students_exact_match
    ON students (first_name, last_name, date_of_birth);

CREATE INDEX idx_students_fuzzy_match
    ON students (last_name, date_of_birth);
