package br.com.mc_manager.server.service.runtime;

import java.util.function.Consumer;

public interface ConsoleGateway {
  void attach(Consumer<String> online);

  void write(String command);

  void detach();
}
