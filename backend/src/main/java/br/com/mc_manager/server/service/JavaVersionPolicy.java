package br.com.mc_manager.server.service;

import java.util.List;

public class JavaVersionPolicy {
  private static final List<Integer> AVAILABLE = List.of(8, 17, 21, 25);

  public static int resolveJavaVersion(String required) {
    int requiredNumber = Integer.parseInt(required);

    return AVAILABLE.stream()
            .filter(v -> v >= requiredNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Sem java disponível para " + required));
  }
}
