package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.service.runtime.ProcessLifecycle;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.WaitResponse;

public class DockerProcessLifecycle implements ProcessLifecycle {

  private final DockerClient dockerClient;
  private final String containerId;

  private volatile Runnable onExitCallback;

  public DockerProcessLifecycle(DockerClient dockerClient, String containerId) {
    this.dockerClient = dockerClient;
    this.containerId = containerId;
  }

  @Override
  public void start() {
    dockerClient.startContainerCmd(containerId).exec();
    watchExit();
  }

  @Override
  public boolean isRunning() {
    InspectContainerResponse info = dockerClient.inspectContainerCmd(containerId).exec();
    return Boolean.TRUE.equals(info.getState().getRunning());
  }

  @Override
  public void stop(int timeoutSeconds) {
    if (!isRunning()) return;

    dockerClient.stopContainerCmd(containerId).withTimeout(timeoutSeconds).exec();
    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
  }

  @Override
  public void cleanup() {
    try {
      dockerClient.removeContainerCmd(containerId).withForce(true).exec();
    } catch (NotFoundException ignored) {
    }
  }

  @Override
  public void onExit(Runnable callback) {
    this.onExitCallback = callback;
  }

  String getContainerId() {
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
