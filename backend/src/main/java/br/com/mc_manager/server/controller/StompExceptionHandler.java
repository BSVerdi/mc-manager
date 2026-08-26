package br.com.mc_manager.server.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class StompExceptionHandler {

  public record StompErrorPayload(String message) {
  }

  @MessageExceptionHandler(SecurityException.class)
  @SendToUser("/queue/errors")
  public StompErrorPayload handleSecurityException(SecurityException ex) {
    return new StompErrorPayload(ex.getMessage());
  }

  @MessageExceptionHandler(IllegalStateException.class)
  @SendToUser("/queue/errors")
  public StompErrorPayload handleIllegalStateException(IllegalStateException ex) {
    return new StompErrorPayload(ex.getMessage());
  }

}
