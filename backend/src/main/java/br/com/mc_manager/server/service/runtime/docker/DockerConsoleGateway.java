package br.com.mc_manager.server.service.runtime.docker;

import br.com.mc_manager.server.service.runtime.ConsoleGateway;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DockerConsoleGateway implements ConsoleGateway {

  private final DockerClient dockerClient;
  private final Supplier<String> containerIdSupplier;

  private volatile Closeable logStream;
  private volatile Closeable attachStream;
  private volatile PipedOutputStream stdinPipe;

  public DockerConsoleGateway(DockerClient dockerClient, Supplier<String> containerIdSupplier) {
    this.dockerClient = dockerClient;
    this.containerIdSupplier = containerIdSupplier;
  }

  @Override
  public void attach(Consumer<String> online) {
    String containerId = containerIdSupplier.get();
    if (containerId == null) {
      throw new IllegalStateException("Container ainda não foi criado.");
    }

    attatchStdin(containerId);
    attatchLogs(containerId, online);
  }

  private void attatchStdin(String containerId) {
    try {
      PipedInputStream pipeIn = new PipedInputStream();
      stdinPipe = new PipedOutputStream(pipeIn);

      attachStream = dockerClient.attachContainerCmd(containerId)
              .withStdIn(pipeIn)
              .withStdOut(false)
              .withStdErr(false)
              .withFollowStream(true)
              .exec(new ResultCallback.Adapter<>());
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao anexar stdion do container " + containerId, e);
    }
  }

  private void attatchLogs(String containerId, Consumer<String> online) {
    logStream = dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTail(0)
            .exec(new ResultCallback.Adapter<Frame>() {
              @Override
              public void onNext(Frame frame) {
                online.accept(new String(frame.getPayload(), StandardCharsets.UTF_8).strip());
              }
            });
  }

  @Override
  public void write(String command) {
    if (stdinPipe == null) {
      throw new IllegalStateException("Console não está anexado");
    }

    try {
      stdinPipe.write((command + "\n").getBytes(StandardCharsets.UTF_8));
      stdinPipe.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Erro ao enviar comando ao container", e);
    }
  }

  @Override
  public void detach() {
    closeQuietly(logStream);
    closeQuietly(attachStream);
    closeQuietly(stdinPipe);

    logStream = null;
    attachStream = null;
    stdinPipe = null;
  }

  private void closeQuietly(Closeable closeable) {
    if (closeable == null) return;

    try {
      closeable.close();
    } catch (IOException ignored) {
    }
  }
}
