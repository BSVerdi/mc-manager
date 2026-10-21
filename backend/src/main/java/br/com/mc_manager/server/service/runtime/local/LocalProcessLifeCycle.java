package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.service.runtime.ProcessLifecycle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalProcessLifeCycle implements ProcessLifecycle {

  private final Server server;
  private final String javaExecutable;
  private final Path serverDirectory;
  private final ExecutorService executor;
  private final LocalServerProcessFactory processFactory;

  private volatile Process process;
  private volatile Runnable onExitCallback;

  public LocalProcessLifeCycle(
          Server server,
          String javaExecutable,
          Path serverDirectory,
          ExecutorService executor,
          LocalServerProcessFactory processFactory
  ) {
    this.server = server;
    this.javaExecutable = javaExecutable;
    this.serverDirectory = serverDirectory;
    this.executor = executor;
    this.processFactory = processFactory;
  }

  @Override
  public synchronized void start() {
    if (isRunning()) throw new IllegalStateException("Servidor já está em execução.");

    try {
      String minRam = server.getMinRam();
      String maxRam = server.getMaxRam();

      process = processFactory.create(javaExecutable, minRam, maxRam, serverDirectory);
      executor.submit(this::watchExit);
    } catch (IOException e) {
      throw new IllegalStateException("Erro ao iniciar processo do servidor " + server.getId(), e);
    }

  }

  @Override
  public boolean isRunning() {
    return process != null && process.isAlive();
  }

  @Override
  public void stop(int timeoutSeconds) {
    if (!isRunning()) return;

    Process p = process;

    try {
      boolean exited = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);

      if (!exited) {
        p.destroyForcibly();
        p.waitFor();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void cleanup() {
  }

  @Override
  public void onExit(Runnable callback) {
    this.onExitCallback = callback;
  }

  public Process getProcess() {
    return process;
  }

  private void watchExit() {
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    if (onExitCallback != null) onExitCallback.run();
  }
}
