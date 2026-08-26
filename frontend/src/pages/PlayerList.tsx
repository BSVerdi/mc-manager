import { useEffect, useState } from "react";
import type { Player } from "../types/Player";
import { api } from "../lib/apiClient";
import { useServerId } from "../hooks/useServerId";
import { UserStar } from "lucide-react";

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

  const handleToggleOp = async (player: Player) => {
    const nextOperatorState = !player.operator;

    setOperatorStatus(player.nickname, nextOperatorState);

    if (nextOperatorState) {
      api.player.opPlayer(serverId, player.nickname);
    } else {
      api.player.deopPlayer(serverId, player.nickname);
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
                    onClick={() => {
                      handleToggleOp(player);
                      player.operator = false;
                    }}
                  >
                    {player.operator ? "de-OP" : "OP"}
                  </button>
                  <button className="ban-btn">Banir</button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
