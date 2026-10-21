package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.AbstractManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ConsoleGateway;
import br.com.mc_manager.server.service.runtime.MetricsCollector;
import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import oshi.SystemInfo;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;

public class LocalManagedServerProcess extends AbstractManagedServerProcess {

  private final String javaExecutablePath;
  private final Path serverDirectory;
  private final LocalServerProcessFactory processFactory;
  private final SystemInfo systemInfo;
  private final Clock clock;

  public LocalManagedServerProcess(
          Server server,
          SimpMessagingTemplate template,
          ExecutorService executor,
          String javaExecutablePath,
          Path serverDirectory,
          PlayerService playerService,
          SystemInfo systemInfo,
          LocalServerProcessFactory processFactory,
          Clock clock
  ) {
    super(server, template, executor, playerService, clock);

    this.javaExecutablePath = javaExecutablePath;
    this.serverDirectory = serverDirectory;
    this.processFactory = processFactory;
    this.systemInfo = systemInfo;
    this.clock = clock;
  }

  @Override
  protected ProcessLifecycle createLifecycle() {
    return new LocalProcessLifeCycle(server, javaExecutablePath, serverDirectory, executor, processFactory);
  }

  @Override
  protected ConsoleGateway createConsoleGateway() {
    return new LocalConsoleGateway((LocalProcessLifeCycle) lifecycle, executor);
  }

  @Override
  protected MetricsCollector createMetricsCollector() {
    return new LocalMetricsCollector(
            server,
            systemInfo,
            (LocalProcessLifeCycle) lifecycle,
            () -> startedAt,
            lineProcessor::getOnlinePlayersCount,
            clock
    );
  }
}
