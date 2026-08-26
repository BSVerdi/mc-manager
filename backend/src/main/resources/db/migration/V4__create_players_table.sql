CREATE TABLE players
(

    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname   VARCHAR(128) NOT NULL UNIQUE,
    icon       VARCHAR(256),
    banned     BOOLEAN DEFAULT false,
    operator   BOOLEAN DEFAULT false,
    last_login TIMESTAMP,
    server_id  BIGINT       NOT NULL,

    FOREIGN KEY (server_id) REFERENCES servers (id)
);