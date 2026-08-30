ALTER TABLE users
    ADD COLUMN profile_picture_url VARCHAR(255) DEFAULT null,
    ADD COLUMN created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at          TIMESTAMP    DEFAULT null
