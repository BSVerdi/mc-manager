import { UserStar } from "lucide-react";
import { useEffect, useState } from "react";
import { useServerId } from "../hooks/useServerId";
import { useServerStatus } from "../hooks/useServerStatus";
import { api } from "../lib/apiClient";
import type { Player } from "../types/Player";
import { Status } from "../types/Status";

function PlayerAvatar({ name }: { name: string }) {
  const [imageFailed, setImageFailed] = useState(false);

  if (imageFailed) {
    return <span>{name.charAt(0).toUpperCase()}</span>;
  }

  return (
    <img
      src={`https://mc-heads.net/avatar/${encodeURIComponent(name)}/40`}
      alt={`Cabeça de ${name}`}
      onError={() => setImageFailed(true)}
      loading="lazy"
    />
  );
}

export default function PlayersList() {
  const serverId = useServerId();
  const { status } = useServerStatus(serverId);
  const [players, setPlayers] = useState<Player[]>([]);

  useEffect(() => {
    const getPlayers = async () => {
      const players = await api.player.getPlayers(serverId);

      setPlayers(players);
    };

    getPlayers();
    const interval = setInterval(getPlayers, 5000);

    return () => clearInterval(interval);
  }, []);

  const setOperatorStatus = (nickname: string, operator: boolean) => {
    setPlayers((old) =>
      old.map((p) => (p.nickname === nickname ? { ...p, operator } : p)),
    );
  };

  const setBannedStatus = (nickname: string, banned: boolean) => {
    setPlayers((old) =>
      old.map((p) => (p.nickname === nickname ? { ...p, banned } : p)),
    );
  };

  const handleToggleOp = async (player: Player) => {
    const nextOperatorState = !player.operator;

    setOperatorStatus(player.nickname, nextOperatorState);

    if (nextOperatorState) {
      api.player.opPlayer(serverId, player.nickname);
    } else {
      api.player.deopPlayer(serverId, player.nickname);
    }
  };

  const handleToggleBan = async (player: Player) => {
    const nextBanState = !player.banned;

    setBannedStatus(player.nickname, nextBanState);

    if (nextBanState) {
      api.player.ban(serverId, player.nickname);
    } else {
      api.player.unban(serverId, player.nickname);
    }
  };

  return (
    <>
      <section className="player-container">
        <ul className="player-list">
          {players.map((player) => (
            <li key={player.nickname} className="player-card">
              <PlayerAvatar name={player.nickname} />
              <div className="player-info">
                <div>
                  <h3>
                    {player.operator ? (
                      <UserStar style={{ width: 16, height: 16 }} />
                    ) : null}{" "}
                    {player.nickname}
                  </h3>
                  <span>{player.online ? "Online" : "Offline"} </span>
                </div>
                <div className="player-commands">
                  <button
                    className={`operator-btn ${player.operator ? "active" : ""}`}
                    onClick={() => handleToggleOp(player)}
                    disabled={status === Status.ONLINE ? false : true}
                  >
                    {player.operator ? "de-OP" : "OP"}
                  </button>
                  <button
                    className="ban-btn"
                    onClick={() => handleToggleBan(player)}
                    disabled={status === Status.ONLINE ? false : true}
                  >
                    {player.banned ? "Desbanir" : "Banir"}
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
