package br.com.mc_manager.server.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ServerType {
  VANILLA("vanilla"),
  PAPER("paper");

  private final String value;

  ServerType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}

