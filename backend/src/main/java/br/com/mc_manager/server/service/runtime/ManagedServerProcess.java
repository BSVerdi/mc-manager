package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.ServerMetricsDTO;
import br.com.mc_manager.server.model.ServerStatus;

import java.time.Instant;

public interface ManagedServerProcess {
  Long getServerId();

  ServerStatus getStatus();

  boolean isRunning();

  Instant getStartedAt();

  Long getMaxRamMb();

  Integer getOnlinePlayersCount();

  ServerMetricsDTO getMetrics();

  void start();

  void stop();

  void restart();

  void stopThen(Runnable onStopped);

  void sendCommand(String command);

  void opPlayer(String nickname);

  void deopPlayer(String nickname);

  void banPlayer(String nickname);

  void unbanPlayer(String nickname);

  void onUnexpectedExit(Runnable callback);
}
