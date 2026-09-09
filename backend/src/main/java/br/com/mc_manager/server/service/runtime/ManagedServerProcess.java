package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.LogLineDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.service.PlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManagedServerProcess {

  private static final Pattern LEVEL_PREFIX_PATTERN =
          Pattern.compile("^(?:\\[\\d{2}:\\d{2}:\\d{2}]|.*?) (\\[.*].*)$");
  private static final Pattern JOIN_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+)\\[/[^\\]]+] logged in with entity id \\d+ at \\(.*\\)");
  private static final Pattern LEAVE_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+) lost connection: .*$");

  private final Server server;
  private final String javaExecutable;
  private final Path serverDirectory;
  private final PlayerService playerService;

  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final Long maxRamMb;

  private final Set<String> onlinePlayers = ConcurrentHashMap.newKeySet();
  private final AtomicLong idCounter = new AtomicLong();
  private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.OFFLINE);

  private volatile Process process;
  private volatile BufferedWriter processInput;
  private volatile Instant startedAt;

  public ManagedServerProcess(
          Server server,
          SimpMessagingTemplate template,
          ExecutorService executor,
          String javaExecutable,
          Path serverDirectory,
          PlayerService playerService
  ) {
    this.server = server;
    this.template = template;
    this.executor = executor;
    this.javaExecutable = javaExecutable;
    this.serverDirectory = serverDirectory;
    this.playerService = playerService;
    this.maxRamMb = parseRamToMb(server.getMaxRam());
  }

  private static Long parseRamToMb(String ram) {
    String trimmed = ram.trim().toUpperCase();

    if (trimmed.endsWith("G")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1)) * 1024;
    }

    if (trimmed.endsWith("M")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
    }

    return Long.parseLong(trimmed);
  }

  public Long getMaxRamMb() {
    return this.maxRamMb;
  }

  public Long getServerId() {
    return server.getId();
  }

  public ServerStatus getStatus() {
    return status.get();
  }

  public Long getProcessPid() {
    return (process != null && process.isAlive()) ? process.pid() : null;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public synchronized void start() {
    if (status.get() == ServerStatus.ONLINE || status.get() == ServerStatus.STARTING) {
      broadcastLine("[CONSOLE] Servidor ja esta rodando ou iniciando");
      return;
    }

    status.set(ServerStatus.STARTING);

    try {
      ProcessBuilder pb = new ProcessBuilder(
              javaExecutable,
              "-Xms" + server.getMinRam(),
              "-Xmx" + server.getMaxRam(),
              "-jar", "server.jar",
              "nogui"
      );

      pb.directory(serverDirectory.toFile());
      pb.redirectErrorStream(true);

      process = pb.start();
      processInput = new BufferedWriter(
              new OutputStreamWriter(
                      process.getOutputStream(),
                      StandardCharsets.UTF_8
              )
      );

      startedAt = Instant.now();
      status.set(ServerStatus.ONLINE);
      broadcastStatus();
      broadcastLine("[CONSOLE] Iniciando servidor");

      executor.submit(this::pumpOutput);
      executor.submit(this::watchProcessExit);

    } catch (IOException e) {
      status.set(ServerStatus.OFFLINE);

      broadcastLine("[CONSOLE] Erro ao iniciar servidor: " + e.getMessage());
      broadcastStatus();
    }
  }

  public synchronized void stop() {
    if (process == null || !process.isAlive()) {
      broadcastLine("[CONSOLE] Servidor não está rodando");
      return;
    }

    status.set(ServerStatus.STOPPING);
    broadcastStatus();

    sendCommand("stop");
    disconnectAllPlayers();
  }

  public synchronized void restart() {
    if (process == null || !process.isAlive()) {
      broadcastLine("[CONSOLE] Não é possível reiniciar servidor offline");
      return;
    }
    broadcastLine("[CONSOLE] Servidor reiniciando");

    stopThen(this::start);
  }

  public synchronized void sendCommand(String command) {
    if (processInput == null || process == null || !process.isAlive()) {
      broadcastLine("[CONSOLE] Não é possível enviar comando: servidor parado");
      return;
    }

    try {
      processInput.write(command);
      processInput.newLine();
      processInput.flush();
    } catch (IOException e) {
      broadcastLine("[CONSOLE] Erro ao enviar comando: " + e.getMessage());
    }
  }

  public void opPlayer(String nickname) {
    playerService.opPlayer(server.getId(), nickname);
    sendCommand("op " + nickname);
  }

  public void deOpPlayer(String nickname) {
    playerService.deopPlayer(server.getId(), nickname);
    sendCommand("deop " + nickname);
  }

  public void banPlayer(String nickname) {
    playerService.banPlayer(server.getId(), nickname);
    sendCommand("ban " + nickname);
  }

  public void unbanPlayer(String nickname) {
    playerService.unbanPlayer(server.getId(), nickname);
    sendCommand("pardon " + nickname);
  }

  public Integer getOnlinePlayersCount() {
    return onlinePlayers.size();
  }

  public synchronized void stopThen(Runnable onStopped) {
    if (process == null || !process.isAlive()) {
      onStopped.run();
      return;
    }

    status.set(ServerStatus.STOPPING);
    broadcastStatus();

    Process processToStop = process;
    sendCommand("stop");
    disconnectAllPlayers();

    executor.submit(() -> {
      try {
        boolean exited = processToStop.waitFor(60, TimeUnit.SECONDS);
        if (!exited) {
          broadcastLine("[CONSOLE] Forçando encerramento...");
          processToStop.destroyForcibly();
          processToStop.waitFor();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      onStopped.run();
    });
  }

  private void watchProcessExit() {
    try {
      int exitCode = process.waitFor();

      broadcastStatus();
      startedAt = null;

      broadcastLine("[CONSOLE] Processo encerrado com código " + exitCode);
      status.set(ServerStatus.OFFLINE);
      broadcastStatus();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void pumpOutput() {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8)
    )) {
      String line;

      while ((line = reader.readLine()) != null) {
        trackPlayerEvents(line);
        broadcastLine(line);
      }

    } catch (IOException e) {
      broadcastLine("[CONSOLE] Stream de saída encerrado " + e.getMessage());
    }
  }

  private String stripDatePrefix(String line) {
    Matcher m = LEVEL_PREFIX_PATTERN.matcher(line);
    return m.matches() ? m.group(1) : line;
  }

  private void trackPlayerEvents(String line) {
    String clean = stripDatePrefix(line);

    Matcher joinMatcher = JOIN_PATTERN.matcher(clean);
    if (joinMatcher.matches()) {
      var playerName = joinMatcher.group(1);

      playerService.saveServerPlayer(server.getId(), playerName);
      playerService.playerConnected(server.getId(), playerName);

      onlinePlayers.add(playerName);

      return;
    }

    Matcher leaverMatcher = LEAVE_PATTERN.matcher(clean);
    if (leaverMatcher.matches()) {
      var playerNickname = leaverMatcher.group(1);

      playerService.playerDisconnected(server.getId(), playerNickname);
      onlinePlayers.remove(playerNickname);
    }

  }

  private void broadcastStatus() {
    template.convertAndSend("/topic/status/" + server.getId(), status.get());
  }

  private void broadcastLine(String rawLine) {
    String clean = stripDatePrefix(rawLine);

    template.convertAndSend("/topic/console/" + server.getId(),
            new LogLineDTO(idCounter.incrementAndGet(), clean, Instant.now().toString())
    );
  }

  private void disconnectAllPlayers() {
    onlinePlayers.forEach((playerName) -> playerService.playerDisconnected(server.getId(), playerName));
    onlinePlayers.clear();
  }

}
