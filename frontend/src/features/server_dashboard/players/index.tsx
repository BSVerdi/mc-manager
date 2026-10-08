import { UserStar } from "lucide-react";
import { memo, useState } from "react";
import { useServerStatus } from "../../../hooks/useServerStatus";
import { Status } from "../../../types/status";
import { useServerId } from "../hooks/useServerId";
import { usePlayersList } from "./hooks/usePlayersList";
import type { Player } from "./types";

const PlayerAvatar = memo(function PlayerAvatar({ name }: { name: string }) {
  const [imageFailed, setImageFailed] = useState(false);

  const initial = name ? name.charAt(0).toUpperCase() : "?";

  if (imageFailed) {
    return <span aria-hidden="true">{initial}</span>;
  }

  return (
    <img
      src={`https://mc-heads.net/avatar/${encodeURIComponent(name)}/40`}
      alt={`Cabeça de ${name}`}
      onError={() => setImageFailed(true)}
      loading="lazy"
    />
  );
});

export function PlayersList() {
  const serverId = useServerId();
  const { status } = useServerStatus(serverId);
  const { pendingNick, players, opPlayer, deopPlayer, banPlayer, unbanPlayer } = usePlayersList(serverId);

  const handleToggleOp = async (player: Player) => {
    if (pendingNick) return;

    const nickname = player.nickname;
    if (player.operator) {
      await deopPlayer(nickname);
    } else {
      await opPlayer(nickname);
    }
  };

  const handleToggleBan = async (player: Player) => {
    if (pendingNick) return;

    const nickname = player.nickname;
    if (player.banned) {
      await unbanPlayer(nickname);
    } else {
      await banPlayer(nickname);
    }
  };

  const isServerOnline = status === Status.ONLINE;

  return (
    <>
      <section className="player-container">
        <ul className="player-list">
          {players.map((player) => {
            const isPending = pendingNick === player.nickname;

            return (
              <li key={player.nickname} className="player-card">
                <PlayerAvatar name={player.nickname} />
                <div className="player-info">
                  <div>
                    <h3>
                      {player.operator ? <UserStar style={{ width: 16, height: 16 }} /> : null} {player.nickname}
                    </h3>
                    <span>{player.online ? "Online" : "Offline"} </span>
                  </div>
                  <div className="player-commands">
                    <button
                      className={`operator-btn ${player.operator ? "active" : ""}`}
                      onClick={() => handleToggleOp(player)}
                      disabled={!isServerOnline || isPending}
                    >
                      {player.operator ? "deOP" : "OP"}
                    </button>
                    <button
                      className="ban-btn"
                      onClick={() => handleToggleBan(player)}
                      disabled={!isServerOnline || isPending}
                    >
                      {player.banned ? "Desbanir" : "Banir"}
                    </button>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      </section>
    </>
  );
}
