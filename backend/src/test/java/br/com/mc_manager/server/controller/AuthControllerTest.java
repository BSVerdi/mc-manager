package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.LoginDTO;
import br.com.mc_manager.server.dto.RegisterUserDTO;
import br.com.mc_manager.server.dto.RegisteredUserDTO;
import br.com.mc_manager.server.dto.TokenDTO;
import br.com.mc_manager.server.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @Nested
  @DisplayName("Métodos de Login e Registro")
  class LoginAndRegisterTests {

    @Test
    @DisplayName("Login bem-sucedido")
    void loginSuccess() throws Exception {
      // Arrange
      TokenDTO tokenDTO = new TokenDTO("exemploAccessToken", "exemploRefreshToken");

      when(authService.login(any(LoginDTO.class))).thenReturn(tokenDTO);

      // Act & Assert
      mockMvc.perform(post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {
                                  "email": "example@email.com",
                                  "password": "userPassword"
                              }
                              """))
              .andExpect(status().isOk())
              .andExpect(cookie().exists("access_token"))
              .andExpect(cookie().exists("refresh_token"));

      verify(authService).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("Login com senha inválida retorna erro")
    void loginWithInvalidPassword() throws Exception {
      // Arrange
      when(authService.login(any(LoginDTO.class))).thenThrow(new BadCredentialsException("Authentication failed"));

      // Act & Assert
      mockMvc.perform(post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {
                                  "email": "example@email.com",
                                  "password": "wrong_password"
                              }
                              """))
              .andExpect(status().is4xxClientError());

      verify(authService).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("Registro de novo usuário")
    void registerNewUser() throws Exception {
      // Arrange
      TokenDTO tokenDTO = new TokenDTO("exemploNovoAccessToken", "exemploNovoRefreshToken");

      RegisteredUserDTO registeredUserDTO = new RegisteredUserDTO(1L, tokenDTO);
      when(authService.registerUser(any(RegisterUserDTO.class))).thenReturn(registeredUserDTO);

      // Act & Assert
      mockMvc.perform(post("/api/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {
                                  "name": "example",
                                  "email": "example@email.com",
                                  "password": "userPassword"
                              }
                              """))
              .andExpect(status().isCreated())
              .andExpect(header().exists("Location"));

      verify(authService).registerUser(any(RegisterUserDTO.class));
    }

    @Test
    @DisplayName("Registro de usuário duplicado")
    void registerDuplicateUser() throws Exception {
      // Arrange
      when(authService.registerUser(any(RegisterUserDTO.class))).thenThrow(new DuplicateKeyException("Email already exists"));

      // Act & Assert
      mockMvc.perform(post("/api/auth/register")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {    
                                  "name": "example",
                                  "email": "example@email.com",
                                  "password": "userPassword3"
                              }
                              """))
              .andExpect(status().isConflict());

      verify(authService).registerUser(any(RegisterUserDTO.class));
    }
  }

  @Nested
  @DisplayName("Refresh Token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Refresh token bem-sucedido")
    void refreshTokenSuccess() throws Exception {
      // Arrange
      Cookie validRefreshToken = new Cookie("refresh_token", "validRefreshToken");

      TokenDTO tokenDTO = new TokenDTO("exemploNovoAccessToken", "exemploNovoRefreshToken");
      when(authService.refreshToken(anyString())).thenReturn(tokenDTO);

      // Act & Assert
      mockMvc.perform(post("/api/auth/refresh-token").cookie(validRefreshToken))
              .andExpect(status().isOk())
              .andExpect(cookie().exists("access_token"))
              .andExpect(cookie().exists("refresh_token"));

      verify(authService).refreshToken(anyString());
    }

    @Test
    @DisplayName("Refresh token com token inválido")
    void refreshTokenWithInvalidToken() throws Exception {
      // Arrange
      Cookie invalidRefreshToken = new Cookie("refresh_token", "invalidRefreshToken");

      when(authService.refreshToken(invalidRefreshToken.getValue())).thenThrow(new BadCredentialsException("Invalid refresh token"));

      // Act & Assert
      mockMvc.perform(post("/api/auth/refresh-token").cookie(invalidRefreshToken))
              .andExpect(status().isUnauthorized());

      verify(authService).refreshToken(anyString());
    }

    @Test
    @DisplayName("Refresh token sem cookie")
    void refreshTokenWithoutCookie() throws Exception {
      // Act & Assert
      mockMvc.perform(post("/api/auth/refresh-token"))
              .andExpect(status().isUnauthorized());

      verify(authService, never()).refreshToken(anyString());
    }
  }

  @Nested
  @DisplayName("Get Me - Informações do Usuário")
  class GetMeTests {

    @Test
    @WithMockUser
    @DisplayName("Obter informações do usuário autenticado")
    void getMeAuthenticated() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/auth/me")
                      .cookie(new Cookie("access_token", "valid_access_token")))
              .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Obter informações com usuário não autenticado")
    void getMeNotAuthenticated() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/api/auth/me"))
              .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("Logout")
  class LogoutTests {

    @Test
    @DisplayName("Logout remove cookies de autenticação")
    void logoutRemovesCookies() throws Exception {
      // Arrange
      String accessToken = "valid_access_token";
      String refreshToken = "valid_refresh_token";

      // Act & Assert
      mockMvc.perform(post("/api/auth/logout")
                      .cookie(new Cookie("access_token", accessToken))
                      .cookie(new Cookie("refresh_token", refreshToken)))
              .andExpect(status().isOk())
              .andExpect(cookie().doesNotExist("access_token"))
              .andExpect(cookie().doesNotExist("refresh_token"));
    }

    @Test
    @DisplayName("Logout sem cookies")
    void logoutWithoutCookies() throws Exception {
      // Act & Assert
      mockMvc.perform(post("/api/auth/logout"))
              .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Segurança e Validações")
  class SecurityAndValidationTests {

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Login com usuário mockado na segurança")
    void loginWithMockedSecurityUser() throws Exception {
      // Arrange
      TokenDTO tokenDTO = new TokenDTO("exemploAccessToken", "exemploRefreshToken");

      when(authService.login(any(LoginDTO.class))).thenReturn(tokenDTO);

      // Act & Assert
      mockMvc.perform(post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("""
                              {
                                  "email": "testuser@example.com",
                                  "password": "password123"
                              }
                              """))
              .andExpect(status().isOk());

      verify(authService).login(any(LoginDTO.class));
    }

    @Test
    @DisplayName("Validação de DTOs no login")
    void loginWithMissingFields() throws Exception {
      // Arrange
      when(authService.login(any())).thenThrow(new RuntimeException("Invalid login credentials"));

      // Act & Assert
      mockMvc.perform(post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{}"))
              .andExpect(status().isBadRequest());

      verify(authService, never()).login(any(LoginDTO.class));
    }
  }
}