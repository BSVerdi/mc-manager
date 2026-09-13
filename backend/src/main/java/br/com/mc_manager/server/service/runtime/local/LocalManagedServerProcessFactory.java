package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import br.com.mc_manager.server.service.runtime.ManagedServerProcess;
import br.com.mc_manager.server.service.runtime.ManagedServerProcessFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.ExecutorService;

@Component
@Profile("!docker")
public class LocalManagedServerProcessFactory implements ManagedServerProcessFactory {

  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final PlayerService playerService;
  private final SystemInfo systemInfo;
  private final DefaultServerProcessFactory processFactory;
  private final Clock clock;

  public LocalManagedServerProcessFactory(SimpMessagingTemplate template, ExecutorService executor, PlayerService playerService, SystemInfo systemInfo, DefaultServerProcessFactory processFactory, Clock clock) {
    this.template = template;
    this.executor = executor;
    this.playerService = playerService;
    this.systemInfo = systemInfo;
    this.processFactory = processFactory;
    this.clock = clock;
  }

  @Override
  public ManagedServerProcess create(Server config, String javaExecutablePath, Path directory) {
    LocalManagedServerProcess process = new LocalManagedServerProcess(
            config,
            template,
            executor,
            javaExecutablePath,
            directory,
            playerService,
            systemInfo,
            processFactory,
            clock
    );

    process.init();
    return process;
  }
}
