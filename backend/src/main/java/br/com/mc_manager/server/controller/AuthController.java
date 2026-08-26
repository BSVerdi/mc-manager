package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.LoginDTO;
import br.com.mc_manager.server.dto.RegisterUserDTO;
import br.com.mc_manager.server.dto.TokenDTO;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public ResponseEntity<Void> login(
          @Valid @RequestBody LoginDTO dto,
          HttpServletResponse response
  ) {
    var token = service.login(dto);

    setAuthCookie(response, token);

    return ResponseEntity.ok().build();
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<Void> refreshToken(
          @CookieValue(name = "refresh_token", required = false) String refreshToken,
          HttpServletResponse response
  ) {
    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    var token = service.refreshToken(refreshToken);

    setAuthCookie(response, token);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserDetailsDTO> getMe(Authentication authentication) {
    var user = service.getMe(authentication.getName());

    return ResponseEntity.ok().body(user);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletResponse response) {
    clearCookie(response, "access_token");
    clearCookie(response, "refresh_token");

    return ResponseEntity.ok().build();
  }

  @PostMapping("/register")
  public ResponseEntity<Void> register(
          @Valid @RequestBody RegisterUserDTO dto,
          HttpServletResponse response,
          UriComponentsBuilder uriBuilder
  ) {
    var registeredUser = service.registerUser(dto);

    setAuthCookie(response, registeredUser.tokenDTO());

    var uri = uriBuilder
            .path("/users/{id}")
            .buildAndExpand(registeredUser.id())
            .toUri();

    return ResponseEntity.created(uri).build();
  }


  private void setAuthCookie(HttpServletResponse response, TokenDTO dto) {
    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("access_token", dto.accessToken(), Duration.ofMinutes(30)).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("refresh_token", dto.refreshToken(), Duration.ofHours(2)).toString());
  }

  private ResponseCookie buildCookie(String name, String token, Duration duration) {
    return ResponseCookie.from(name, token)
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/")
            .maxAge(duration)
            .build();
  }

  private void clearCookie(HttpServletResponse response, String name) {
    ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

}
