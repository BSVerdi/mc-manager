package br.com.mc_manager.server.service.runtime.local;

import br.com.mc_manager.server.service.runtime.ConsoleGateway;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class LocalConsoleGateway implements ConsoleGateway {

  private final LocalProcessLifeCycle lifeCycle;
  private final ExecutorService executor;

  private volatile BufferedWriter processInput;

  public LocalConsoleGateway(LocalProcessLifeCycle lifeCycle, ExecutorService executor) {
    this.lifeCycle = lifeCycle;
    this.executor = executor;
  }


  @Override
  public void attach(Consumer<String> online) {
    Process process = lifeCycle.getProcess();

    if (process == null) {
      throw new IllegalStateException("Processo ainda não foi iniciado");
    }

    processInput = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)
    );

    executor.submit(() -> pumpOutput(process, online));
  }

  private void pumpOutput(Process process, Consumer<String> online) {
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))
    ) {
      String line;

      while ((line = reader.readLine()) != null) {
        online.accept(line);
      }
    } catch (IOException e) {
      online.accept("[CONSOLE] Stream de saída encerrado " + e.getMessage());
    }
  }

  @Override
  public void write(String command) {
    if (processInput == null) {
      throw new IllegalStateException("Console não está anexado");
    }

    try {
      processInput.write(command);
      processInput.newLine();
      processInput.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Erro ao enviar comando ao processo", e);
    }
  }

  @Override
  public void detach() {
    if (processInput != null) {
      try {
        processInput.close();
      } catch (IOException ignored) {
      }
      processInput = null;
    }
  }
}
