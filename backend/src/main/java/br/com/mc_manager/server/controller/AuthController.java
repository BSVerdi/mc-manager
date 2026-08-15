package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.LoginDTO;
import br.com.mc_manager.server.dto.RefreshTokenDTO;
import br.com.mc_manager.server.dto.TokenDTO;
import br.com.mc_manager.server.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  @Autowired
  private AuthenticationService service;

  @PostMapping("/login")
  public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO dto) {
    var token = service.login(dto);

    return ResponseEntity.ok(token);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<TokenDTO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
    var token = service.refreshToken(dto);

    return ResponseEntity.ok(token);
  }

}
