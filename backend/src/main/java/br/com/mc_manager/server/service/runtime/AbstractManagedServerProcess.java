package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static br.com.mc_manager.server.service.runtime.util.RamUnitParser.toMb;

public abstract class AbstractManagedServerProcess implements ManagedServerProcess {

  protected final Server server;
  protected final ExecutorService executor;
  protected final PlayerService playerService;
  protected final Clock clock;
  protected final ServerBroadcaster broadcaster;
  protected final ServerConsoleLineProcessor lineProcessor;
  protected final long maxRamMb;

  protected final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.OFFLINE);

  protected ProcessLifecycle lifecycle;
  protected ConsoleGateway consoleGateway;
  protected MetricsCollector metricsCollector;

  protected volatile Instant startedAt;
  private volatile Runnable unexpectedExitCallback;

  public AbstractManagedServerProcess(
          Server server,
          SimpMessagingTemplate template,
          ExecutorService executor,
          PlayerService playerService,
          Clock clock
  ) {
    this.server = server;
    this.executor = executor;
    this.playerService = playerService;
    this.clock = clock;
    this.maxRamMb = toMb(server.getMaxRam());

    this.broadcaster = new ServerBroadcaster(server.getId(), template, clock);
    this.lineProcessor = new ServerConsoleLineProcessor(server.getId(), broadcaster, playerService);
  }

  public final void init() {
    this.lifecycle = createLifecycle();
    this.consoleGateway = createConsoleGateway();
    this.metricsCollector = createMetricsCollector();

    lifecycle.onExit(this::handleUnexpectedExit);
  }

  protected abstract ProcessLifecycle createLifecycle();

  protected abstract ConsoleGateway createConsoleGateway();

  protected abstract MetricsCollector createMetricsCollector();

  @Override
  public Long getServerId() {
    return server.getId();
  }

  @Override
  public ServerStatus getStatus() {
    return status.get();
  }

  @Override
  public boolean isRunning() {
    return lifecycle.isRunning();
  }

  @Override
  public Instant getStartedAt() {
    return startedAt;
  }

  @Override
  public Long getMaxRamMb() {
    return maxRamMb;
  }

  @Override
  public Integer getOnlinePlayersCount() {
    return lineProcessor.getOnlinePlayersCount();
  }

  @Override
  public ServerMetricsDTO getMetrics() {
    return metricsCollector.collect();
  }

  @Override
  public synchronized void start() {
    if (status.get() == ServerStatus.ONLINE || status.get() == ServerStatus.STARTING) {
      broadcaster.consoleLine(LogLevel.CONSOLE, "Servidor já está rodando ou iniciando");
      return;
    }

    status.set(ServerStatus.STARTING);
    broadcaster.status(status.get());

    try {
      lifecycle.start();
      consoleGateway.attach(lineProcessor);

      startedAt = Instant.now();

      status.set(ServerStatus.ONLINE);
      broadcaster.status(status.get());

      broadcaster.consoleLine(LogLevel.CONSOLE, "Iniciando servidor");
    } catch (Exception e) {
      status.set(ServerStatus.OFFLINE);
      broadcaster.status(status.get());

      broadcaster.consoleLine(LogLevel.CONSOLE, "Erro ao iniciar servidor: " + e.getMessage());
    }
  }

  @Override
  public synchronized void stop() {
    if (!lifecycle.isRunning()) {
      broadcaster.consoleLine(LogLevel.CONSOLE, "Servidor não está rodando");
      return;
    }

    stopInternal(() -> {
    });
  }

  @Override
  public synchronized void restart() {
    if (!lifecycle.isRunning()) {
      broadcaster.consoleLine(LogLevel.CONSOLE, "Não é possível reiniciar servidor offline");
      return;
    }

    broadcaster.consoleLine(LogLevel.CONSOLE, "Servidor reiniciando");
    stopThen(this::start);
  }

  @Override
  public synchronized void stopThen(Runnable onStopped) {
    if (!lifecycle.isRunning()) {
      onStopped.run();
      return;
    }

    stopInternal(onStopped);
  }

  private void stopInternal(Runnable onStopped) {
    status.set(ServerStatus.STOPPING);
    broadcaster.status(status.get());

    consoleGateway.write("stop");
    lineProcessor.disconnectAllPlayers();

    executor.submit(() -> {
      lifecycle.stop(60);
      consoleGateway.detach();
      startedAt = null;

      status.set(ServerStatus.OFFLINE);
      broadcaster.status(status.get());

      onStopped.run();
    });
  }

  @Override
  public synchronized void sendCommand(String command) {
    if (!lifecycle.isRunning()) {
      broadcaster.consoleLine(LogLevel.CONSOLE, "Não é possível enviar comando: servidor parado");
      return;
    }

    try {
      consoleGateway.write(command);
    } catch (Exception e) {
      broadcaster.consoleLine(LogLevel.CONSOLE, "Erro ao enviar comando: " + e.getMessage());
    }
  }

  @Override
  public void opPlayer(String nickname) {
    playerService.opPlayer(server.getId(), nickname);
    sendCommand("op " + nickname);
  }

  @Override
  public void deopPlayer(String nickname) {
    playerService.deopPlayer(server.getId(), nickname);
    sendCommand("deop " + nickname);
  }

  @Override
  public void banPlayer(String nickname) {
    playerService.banPlayer(server.getId(), nickname);
    sendCommand("ban " + nickname);
  }

  @Override
  public void unbanPlayer(String nickname) {
    playerService.unbanPlayer(server.getId(), nickname);
    sendCommand("pardon " + nickname);
  }

  @Override
  public void onUnexpectedExit(Runnable callback) {
    this.unexpectedExitCallback = callback;
  }

  private void handleUnexpectedExit() {
    if (status.compareAndSet(ServerStatus.ONLINE, ServerStatus.OFFLINE)
            || status.compareAndSet(ServerStatus.STARTING, ServerStatus.OFFLINE)
    ) {
      startedAt = null;
      broadcaster.consoleLine(LogLevel.CONSOLE, "Processo encerrado inesperadamente");
      broadcaster.status(status.get());

      lifecycle.cleanup();

      if (unexpectedExitCallback != null) {
        unexpectedExitCallback.run();
      }
    }
  }
}
