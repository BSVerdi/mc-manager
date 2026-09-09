package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.PlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Component
public class DefaultManagedServerProcessFactory implements ManagedServerProcessFactory {
  private final SimpMessagingTemplate template;
  private final ExecutorService executor;
  private final PlayerService playerService;

  public DefaultManagedServerProcessFactory(SimpMessagingTemplate template, ExecutorService executor, PlayerService playerService) {
    this.template = template;
    this.executor = executor;
    this.playerService = playerService;
  }

  @Override
  public ManagedServerProcess create(Server config, String javaExecutablePath, Path directory) {
    return new ManagedServerProcess(config, template, executor, javaExecutablePath, directory, playerService);
  }
}
