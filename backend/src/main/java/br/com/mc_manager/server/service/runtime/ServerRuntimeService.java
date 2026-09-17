package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.server.ServerMetricsDTO;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerStatus;
import br.com.mc_manager.server.repository.ServerRepository;
import br.com.mc_manager.server.service.files.ServerFilesService;
import com.github.dockerjava.api.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service

public class ServerRuntimeService {

  private final Map<Long, ManagedServerProcess> instances = new ConcurrentHashMap<>();

  private final ServerRepository serverRepository;
  private final ServerFilesService serverFilesService;
  private final ManagedServerProcessFactory processFactory;

  @Value("${mcmanager.java_8}")
  private String java8;
  @Value("${mcmanager.java_21}")
  private String java21;
  @Value("${mcmanager.java_25}")
  private String java25;

  public ServerRuntimeService(ServerRepository serverRepository, ServerFilesService serverFilesService, ManagedServerProcessFactory processFactory) {
    this.serverRepository = serverRepository;
    this.serverFilesService = serverFilesService;
    this.processFactory = processFactory;
  }

  public void start(Long serverId) {
    getOrCreateInstance(serverId).start();
  }

  public void stop(Long serverId) {
    getExistingInstance(serverId).ifPresentOrElse(
            instance -> instance.stopThen(() -> discard(serverId)),
            () -> {
              throw new NotFoundException("Server not found");
            }
    );
  }

  public void restart(Long serverId) {
    getOrCreateInstance(serverId).restart();
  }

  public void sendCommand(Long serverId, String command) {
    executeAnInstanceMethod(serverId, (server) -> server.sendCommand(command));
  }

  public void opPlayer(Long serverId, String nickname) {
    executeAnInstanceMethod(serverId, (server) -> server.opPlayer(nickname));
  }

  public void deopPlayer(Long serverId, String nickname) {
    executeAnInstanceMethod(serverId, (server) -> server.deopPlayer(nickname));
  }

  public void banPlayer(Long serverId, String nickname) {
    executeAnInstanceMethod(serverId, (server) -> server.banPlayer(nickname));
  }

  public void unbanPlayer(Long serverId, String nickname) {
    executeAnInstanceMethod(serverId, (server) -> server.unbanPlayer(nickname));
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

  public ServerMetricsDTO getMetrics(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getMetrics)
            .orElse(null);
  }

  public void discard(Long serverId) {
    instances.remove(serverId);
  }

  public Long getMaxRamMb(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getMaxRamMb)
            .orElse(0L);
  }

  public Instant getStartedAt(Long serverId) {
    return getExistingInstance(serverId)
            .map(ManagedServerProcess::getStartedAt)
            .orElse(Instant.now());
  }

  public Set<Long> getActiveServersIds() {
    return instances.entrySet().stream()
            .filter(entry -> entry.getValue().isRunning())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
  }

  public void restartWithUpdatedConfig(Long serverId) {
    var current = getExistingInstance(serverId).orElse(null);

    if (current == null || !current.isRunning()) {
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

      var directory = serverFilesService.getServerDirectory(id);
      var javaExecutablePath = selectJavaExecutablePath(config.getJavaVersion());

      var instance = processFactory.create(config, javaExecutablePath, directory);
      instance.onUnexpectedExit(() -> discard(serverId));

      return instance;
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

  private void executeAnInstanceMethod(Long serverId, Consumer<ManagedServerProcess> action) {
    ManagedServerProcess server = getExistingInstance(serverId)
            .orElseThrow(() -> new IllegalStateException("Não é possível realizar a ação em um servidor fechado"));

    action.accept(server);
  }

}
