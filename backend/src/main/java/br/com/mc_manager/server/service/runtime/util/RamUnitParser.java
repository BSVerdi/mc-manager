package br.com.mc_manager.server.service.runtime.util;

public class RamUnitParser {

  private RamUnitParser() {
  }

  public static long toMb(String ram) {
    String trimmed = ram.trim().toUpperCase();

    if (trimmed.endsWith("G")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1)) * 1024;
    }

    if (trimmed.endsWith("M")) {
      return Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
    }

    return Long.parseLong(trimmed);
  }
}
