CREATE TABLE players_new
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(128) NOT NULL UNIQUE,
    icon     VARCHAR(256)
);

CREATE TABLE server_players
(
    server_id  BIGINT  NOT NULL,
    player_id  BIGINT  NOT NULL,
    banned     BOOLEAN NOT NULL DEFAULT FALSE,
    operator   BOOLEAN NOT NULL DEFAULT FALSE,
    online     BOOLEAN NOT NULL DEFAULT FALSE,
    last_login TIMESTAMP,

    PRIMARY KEY (server_id, player_id),
    CONSTRAINT fk_sp_server FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_player FOREIGN KEY (player_id) REFERENCES players_new (id) ON DELETE CASCADE
);

INSERT INTO players_new (nickname, icon)
SELECT DISTINCT nickname, icon
FROM players;

INSERT INTO server_players (server_id, player_id, banned, operator, online, last_login)
SELECT p.server_id, pn.id, p.banned, p.operator, p.online, p.last_login
FROM players p
         JOIN players_new pn ON pn.nickname = p.nickname;

DROP TABLE players;
RENAME TABLE players_new TO players;