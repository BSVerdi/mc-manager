ALTER TABLE servers
    ADD version      VARCHAR(64) NOT NULL,
    ADD java_version VARCHAR(64) NOT NULL DEFAULT 'JAVA_21';
