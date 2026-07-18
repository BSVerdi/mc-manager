package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.LogLineDTO;
import br.com.mc_manager.server.model.ServerStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MinecraftServerService {

  private final SimpMessagingTemplate template;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final AtomicLong idCounter = new AtomicLong();

  private static final Pattern LEVEL_PREFIX_PATTERN =
          Pattern.compile("^.*?(\\[(?:INFO|WARN|ERROR|FATAL|DEBUG|TRACE)].*)$");

  private static final Pattern JOIN_PATTERN = Pattern.compile("^(\\w+)\\[/[^\\]]+] logged in with entity id \\d+ at \\(.*\\)$");
  private static final Pattern LEAVE_PATTERN = Pattern.compile("^(\\w+) lost connection: .*$");

  private List<String> onlinePlayers = new ArrayList<>();

  @Value("${mcmanager.server-directory}")
  private String serverDirectory;

  @Value("${mcmanager.jar-name}")
  private String jarName;

  @Value("${mcmanager.min-ram}")
  private String minRam;

  @Value("${mcmanager.max-ram}")
  private String maxRam;

  @Value("${mcmanager.java-executable}")
  private String javaExecutable;

  private volatile Process process;
  private volatile BufferedWriter processInput;
  private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.OFFLINE);

  private volatile Instant startedAt;

  public MinecraftServerService(SimpMessagingTemplate template) {
    this.template = template;
  }

  public ServerStatus getStatus() {
    return status.get();
  }

  public Long getProcessPid() {
    return process != null ? process.pid() : null;
  }

  public synchronized void start() {
    if (status.get() == ServerStatus.ONLINE || status.get() == ServerStatus.STARTING) {
      broadcastLine("[CONSOLE] Servidor ja esta rodando ou iniciando.");
      return;
    }

    status.set(ServerStatus.STARTING);

    try {
      ProcessBuilder pb = new ProcessBuilder(
              javaExecutable,
              "-Xms" + minRam,
              "-Xmx" + maxRam,
              "-jar", jarName,
              "nogui"
      );
      pb.directory(new File(serverDirectory));
      pb.redirectErrorStream(true);

      process = pb.start();
      processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

      startedAt = Instant.now();
      status.set(ServerStatus.ONLINE);
      broadcastStatus();
      broadcastLine("[CONSOLE] Iniciando servidor...");

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
      broadcastLine("[CONSOLE] Servidor não esta rodando.");
      return;
    }
    status.set(ServerStatus.STOPPING);
    broadcastStatus();
    sendCommand("stop");
  }

  public synchronized void restart() {
    if (process == null || !process.isAlive()) {
      broadcastLine("[CONSOLE] Servidor não esta rodando.");
      return;
    }

    status.set(ServerStatus.RESTARTING);
    broadcastStatus();
    broadcastLine("[CONSOLE] Reiniciando servidor...");

    Process processToStop = process;

    sendCommand("stop");

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

      start();
    });
  }

  public synchronized void sendCommand(String command) {
    if (processInput == null || process == null || !process.isAlive()) {
      broadcastLine("[CONSOLE] Não e possivel enviar comando: servidor parado.");
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

  private void pumpOutput() {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        trackPlayerEvents(line);

        broadcastLine(line);
      }
    } catch (IOException e) {
      broadcastLine("[CONSOLE] Stream de saida encerrado: " + e.getMessage());
    }
  }

  private void watchProcessExit() {
    try {
      int exitCode = process.waitFor();

      status.set(ServerStatus.OFFLINE);
      startedAt = null;

      broadcastLine("[CONSOLE] Processo encerrado com codigo " + exitCode);
      broadcastStatus();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private String stripDatePrefix(String line) {
    Matcher m = LEVEL_PREFIX_PATTERN.matcher(line);
    return m.matches() ? m.group(1) : line;
  }

  private void broadcastLine(String rawLine) {
    String clean = stripDatePrefix(rawLine);

    template.convertAndSend("/topic/console",
            new LogLineDTO(idCounter.incrementAndGet(), clean, Instant.now().toString()));
  }

  private void broadcastStatus() {
    template.convertAndSend("/topic/status", status.get());
  }

  private Long parseRamToMb(String ram) {
    String trimmed = ram.trim().toUpperCase();

    if (trimmed.endsWith("G")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1)) * 1024;
    }

    if (trimmed.endsWith("M")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
    }

    return Long.parseLong(trimmed);
  }

  private void trackPlayerEvents(String message) {
    String clean = stripDatePrefix(message).replace("[INFO]", "").trim();
    Matcher joinMatcher = JOIN_PATTERN.matcher(clean);

    if (joinMatcher.matches()) {
      onlinePlayers.add(joinMatcher.group(1));
      return;
    }

    Matcher leaveMatcher = LEAVE_PATTERN.matcher(clean);

    if (leaveMatcher.matches()) {
      onlinePlayers.remove(leaveMatcher.group(1));
      return;
    }
  }

  public Long getMaxRamMb() {
    return parseRamToMb(maxRam);
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public List<String> getOnlinePlayers() {
    return onlinePlayers;
  }
}