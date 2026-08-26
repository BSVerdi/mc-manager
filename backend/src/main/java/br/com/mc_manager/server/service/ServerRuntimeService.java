package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ServerManagerService {

  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final Map<Long, ManagedServerProcess> instances = new ConcurrentHashMap<>();

  private final ServerRepository serverRepository;
  private final ServerFileProvisioningService provisioningService;
  private final SimpMessagingTemplate template;
  private final PlayerService playerService;

  @Value("${mcmanager.java-8}")
  private String java8;
  @Value("${mcmanager.java-21}")
  private String java21;
  @Value("${mcmanager.java-25}")
  private String java25;

  public ServerManagerService(ServerRepository serverRepository, ServerFileProvisioningService provisioningService, SimpMessagingTemplate template, PlayerService playerService) {
    this.serverRepository = serverRepository;
    this.provisioningService = provisioningService;
    this.template = template;
    this.playerService = playerService;
  }

  public void start(Long serverId) {
    getOrCreateInstance(serverId).start();
  }

  public void stop(Long serverId) {
    getOrCreateInstance(serverId).stop();
  }

  public void restart(Long serverId) {
    getOrCreateInstance(serverId).restart();
  }

  public void sendCommand(Long serverId, String command) {
    getOrCreateInstance(serverId).sendCommand(command);
  }

  public void opPlayer(Long serverId, String nickname) {
    getOrCreateInstance(serverId).opPlayer(nickname);
  }

  public void deopPlayer(Long serverId, String nickname) {
    getOrCreateInstance(serverId).deOpPlayer(nickname);
  }

  public ServerStatus getStatus(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getStatus)
            .orElse(ServerStatus.OFFLINE);
  }

  public Integer getOnlinePlayersCount(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getOnlinePlayersCount)
            .orElse(0);
  }

  public Long getProcessPid(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getProcessPid)
            .orElse(null);
  }

  public void discard(Long serverId) {
    instances.remove(serverId);
  }

  public Long getMaxRamMb(Long serverId) {
    return getExistingInstance(serverId).map(ManagedServerProcess::getMaxRamMb).orElse(0L);
  }

  public Instant getStartedAt(Long serverId) {
    return getExistingInstance(serverId).map(ManagedServerProcess::getStartedAt).orElse(null);
  }

  public List<String> getOnlinePlayersList(Long serverId) {
    return getExistingInstance(serverId).map(ManagedServerProcess::getOnlinePlayersList).orElse(List.of());
  }

  public Set<Long> getActiveServersIds() {
    return instances.entrySet().stream()
            .filter(entry -> entry.getValue().getProcessPid() != null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
  }

  public void restartWithUpdatedConfig(Long serverId) {
    var current = getExistingInstance(serverId).orElse(null);

    if (current == null || current.getProcessPid() == null) {
      discard(serverId);
      return;
    }

    current.stopThen(() -> {
      discard(serverId);
      start(serverId);
    });
  }

  private Optional<ManagedServerProcess> getExistingInstance(Long serverId) {
    return Optional.ofNullable(instances.get(serverId));
  }

  private ManagedServerProcess getOrCreateInstance(Long serverId) {
    return instances.computeIfAbsent(serverId, id -> {
      Server config = serverRepository.findById(id).orElseThrow(
              () -> new IllegalStateException("Servidor " + id + " não encontrado")
      );

      var directory = provisioningService.resolveServerDirectory(id);
      var javaExecutablePath = selectJavaExecutablePath(config.getJavaVersion());

      return new ManagedServerProcess(
              config,
              template,
              executor,
              javaExecutablePath,
              directory,
              playerService
      );
    });
  }

  private String selectJavaExecutablePath(String javaVersion) {
    return switch (javaVersion) {
      case "JAVA_8" -> java8;
      case "JAVA_21" -> java21;
      case "JAVA_25" -> java25;
      default -> java25;
    };
  }

  public void assertOwnership(Server server, Principal principal) {
    boolean isOwner = !server.getOwner().getEmail().equals(principal.getName());

    if (isOwner) {
      throw new SecurityException("Voce não tem permissão para acessar esse arquivo");
    }
  }

}
