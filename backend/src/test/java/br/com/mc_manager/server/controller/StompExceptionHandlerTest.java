package br.com.mc_manager.server.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StompExceptionHandlerTest {

  private final StompExceptionHandler handler = new StompExceptionHandler();

  @Test
  @DisplayName("Deve converter SecurityException em payload de erro")
  void deveConverterSecurityExceptionEmPayload() {
    SecurityException ex = new SecurityException("Você não tem permissão para essa ação");

    StompExceptionHandler.StompErrorPayload payload = handler.handleSecurityException(ex);

    assertThat(payload.message()).isEqualTo("Você não tem permissão para essa ação");
  }

  @Test
  @DisplayName("Deve converter IllegalStateException em payload de erro")
  void deveConverterIllegalStateExceptionEmPayload() {
    IllegalStateException ex = new IllegalStateException("Servidor fechado");

    StompExceptionHandler.StompErrorPayload payload = handler.handleIllegalStateException(ex);

    assertThat(payload.message()).isEqualTo("Servidor fechado");
  }
}