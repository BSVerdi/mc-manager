package br.com.mc_manager.server.service.runtime;

public enum LogLevel {
  CONSOLE,
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  FATAL,
  UNKNOWN;

  static LogLevel from(String s) {
    if (s == null) return UNKNOWN;

    try {
      return valueOf(s);
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }
}
