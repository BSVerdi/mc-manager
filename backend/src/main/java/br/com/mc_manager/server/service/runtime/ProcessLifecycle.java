package br.com.mc_manager.server.service.runtime;

public interface ProcessLifecycle {
  void start();

  boolean isRunning();

  void stop(int timeoutSeconds);

  void cleanup();

  void onExit(Runnable callback);
}
