package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.WaitResponse;

import java.util.function.Supplier;

public class DockerProcessLifecycle implements ProcessLifecycle {

  private final DockerClient dockerClient;
  private final Supplier<String> create;

  private volatile String containerId;
  private volatile Runnable onExitCallback;

  public DockerProcessLifecycle(DockerClient dockerClient, Supplier<String> create) {
    this.dockerClient = dockerClient;
    this.create = create;
  }

  @Override
  public void start() {
    if (isRunning()) throw new IllegalStateException("Servidor já está em execução");

    containerId = create.get();

    try {
      dockerClient.startContainerCmd(containerId).exec();
    } catch (RuntimeException e) {
      cleanup();
      throw e;
    }

    watchExit();
  }

  @Override
  public boolean isRunning() {
    String id = containerId;
    if (id == null) return false;

    try {
      InspectContainerResponse info = dockerClient.inspectContainerCmd(id).exec();
      return Boolean.TRUE.equals(info.getState().getRunning());
    } catch (NotFoundException e) {
      return false;
    }
  }

  @Override
  public void stop(int timeoutSeconds) {
    String id = containerId;
    if (id == null) return;

    try {
      if (!isRunning()) return;
      dockerClient.stopContainerCmd(id).withTimeout(timeoutSeconds).exec();
    } finally {
      cleanup();
    }
  }

  @Override
  public void cleanup() {
    String id = containerId;
    if (id == null) return;

    try {
      dockerClient.removeContainerCmd(id).withForce(true).exec();
    } catch (NotFoundException ignored) {
    }
  }

  @Override
  public void onExit(Runnable callback) {
    this.onExitCallback = callback;
  }

  public String getContainerId() {
    return containerId;
  }

  private void watchExit() {
    dockerClient.waitContainerCmd(containerId).exec(new ResultCallback.Adapter<WaitResponse>() {
      @Override
      public void onNext(WaitResponse waitResponse) {
        if (onExitCallback != null) onExitCallback.run();
      }
    });
  }
}
