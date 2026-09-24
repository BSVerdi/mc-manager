import { useCallback, useEffect, useState } from "react";
import type { Player } from "../../../types/player";
import { playersApi } from "./api";
import { useOnlinePlayers } from "./useOnlinePlayers";

export function usePlayersList(serverId: number) {
  const [players, setPlayers] = useState<Player[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingNick, setPendingNick] = useState<string | null>(null);

  const onlinePlayers = useOnlinePlayers();

  const getPlayers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const players = await playersApi.getPlayers(serverId);
      setPlayers(players);
    } catch (err) {
      console.error("[usePlayersList] Erro ao carregar os players:", err);
      setError("Não foi possível carregar a lista de players.");
    } finally {
      setLoading(false);
    }
  }, [serverId]);

  useEffect(() => {
    getPlayers();
  }, [getPlayers, onlinePlayers]);

  const opPlayer = useCallback(
    async (nickname: string): Promise<boolean> => {
      setPendingNick(nickname);

      try {
        await playersApi.opPlayer(serverId, nickname);
        await getPlayers();

        return true;
      } catch (err) {
        console.error(`[usePlayersList] Erro ao realizar op do player ${nickname}:`, err);
        setError(`Falha ao atribuir op de ${nickname}`);

        return false;
      } finally {
        setPendingNick(null);
      }
    },
    [serverId, getPlayers],
  );

  const deopPlayer = useCallback(
    async (nickname: string): Promise<boolean> => {
      setPendingNick(nickname);

      try {
        await playersApi.deopPlayer(serverId, nickname);
        await getPlayers();

        return true;
      } catch (err) {
        console.error(`[usePlayersList] Erro ao realizar deop do player ${nickname}:`, err);
        setError(`Falha ao remover op de ${nickname}`);

        return false;
      } finally {
        setPendingNick(null);
      }
    },
    [serverId, getPlayers],
  );

  const banPlayer = useCallback(
    async (nickname: string): Promise<boolean> => {
      setPendingNick(nickname);

      try {
        await playersApi.ban(serverId, nickname);
        await getPlayers();

        return true;
      } catch (err) {
        console.error(`[usePlayersList] Erro ao banir o jogador ${nickname}:`, err);
        setError(`Falha ao banir ${nickname}`);

        return false;
      } finally {
        setPendingNick(null);
      }
    },
    [serverId, getPlayers],
  );

  const unbanPlayer = useCallback(
    async (nickname: string): Promise<boolean> => {
      setPendingNick(nickname);

      try {
        await playersApi.unban(serverId, nickname);
        await getPlayers();

        return true;
      } catch (err) {
        console.error(`[usePlayersList] Erro ao desbanir o jogador ${nickname}:`, err);
        setError(`Falha ao desbanir ${nickname}`);

        return false;
      } finally {
        setPendingNick(null);
      }
    },
    [serverId, getPlayers],
  );

  return {
    loading,
    error,
    pendingNick,
    players,
    opPlayer,
    deopPlayer,
    banPlayer,
    unbanPlayer,
  };
}
