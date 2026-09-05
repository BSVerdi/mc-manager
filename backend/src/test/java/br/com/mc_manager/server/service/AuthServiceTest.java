package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.*;
import br.com.mc_manager.server.factory.UserFactory;
import br.com.mc_manager.server.model.Profile;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private Authentication authentication;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private TokenService tokenService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Captor
  private ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationTokenCaptor;

  @InjectMocks
  private AuthService authService;

  private User validUser;
  private LoginDTO loginDTO;
  private RegisterUserDTO registerUserDTO;

  @BeforeEach
  public void setUp() {
    // Arrange - Preparar mocks e objetos de teste
    this.validUser = UserFactory.createUserWithAllAttributes();

    this.loginDTO = new LoginDTO("example@email.com", "correctPassword");

    this.registerUserDTO = new RegisterUserDTO("example", "example@email.com", "userPassword");

  }

  @Nested
  class Login {

    @Test
    @DisplayName("Deve retornar TokenDTO com sucesso ao login com credenciais válidas")
    void testLoginSuccess() {
      // Arrange
      when(authenticationManager.authenticate(authenticationTokenCaptor.capture())).thenReturn(authentication);
      when(authentication.getPrincipal()).thenReturn(validUser);
      when(tokenService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
      when(tokenService.generateRefreshToken(any(User.class))).thenReturn("refresh-token-456");

      // Act
      TokenDTO result = authService.login(loginDTO);

      // Assert
      assertNotNull(result);
      assertEquals("access-token-123", result.accessToken());
      assertEquals("refresh-token-456", result.refreshToken());

      // Verificar que o método authenticate foi chamado com os argumentos corretos
      verify(authenticationManager, times(1)).authenticate(authenticationTokenCaptor.capture());
      UsernamePasswordAuthenticationToken captured = authenticationTokenCaptor.getValue();
      assertEquals(loginDTO.email(), captured.getPrincipal());
      assertEquals(loginDTO.password(), captured.getCredentials());
    }

    @Test
    @DisplayName("Deve lançar exceção ao login com email inválido")
    void testLoginWithEmailInvalid() {
      // Arrange
      when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Email inválido"));

      // Act & Assert
      assertThatThrownBy(() -> authService.login(loginDTO))
              .isInstanceOf(BadCredentialsException.class)
              .hasMessageContaining("Email inválido");
    }

    @Test
    @DisplayName("Deve lançar exceção ao login com senha inválida")
    void testLoginWithPasswordInvalid() {
      // Arrange
      LoginDTO invalidDTO = new LoginDTO("example@email.com", "wrongPassword");
      when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Senha inválida"));

      // Act & Assert
      assertThatThrownBy(() -> authService.login(invalidDTO))
              .isInstanceOf(BadCredentialsException.class)
              .hasMessageContaining("Senha inválida");
    }
  }

  @Nested
  class RefreshToken {

    @Test
    @DisplayName("Deve retornar novo TokenDTO com acesso e refresh token atualizados")
    void testRefreshTokenSuccess() {
      // Arrange
      when(tokenService.verifyRefreshToken(anyString())).thenReturn(validUser.getEmail());
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(validUser));
      when(tokenService.generateAccessToken(any(User.class))).thenReturn("access-token-new");
      when(tokenService.generateRefreshToken(any(User.class))).thenReturn("refresh-token-new");

      //act
      TokenDTO refreshToken = authService.refreshToken("refresh-token-456");

      // Assert
      assertNotNull(refreshToken);
      assertEquals("access-token-new", refreshToken.accessToken());
      assertEquals("refresh-token-new", refreshToken.refreshToken());

      // Verificar que o email do user foi usado para buscar no repository
      verify(userRepository, times(1)).findByEmailIgnoreCase(eq(validUser.getEmail()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar usar refresh token inválido")
    void testRefreshTokenInvalidToken() {
      // Arrange
      when(tokenService.verifyRefreshToken(anyString())).thenThrow(new RuntimeException("Token expirado"));

      // Act & Assert
      assertThatThrownBy(() -> authService.refreshToken("invalid-refresh-token"))
              .isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Token expirado");
    }
  }

  @Nested
  class GetMe {
    private UserDetailsDTO userDetailsDTO;

    @BeforeEach
    void setupGetMeMocks() {
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(validUser));
    }

    @Test
    @DisplayName("Deve retornar UserDetailsDTO com todos os campos do usuário")
    void testGetMeSuccess() {
      // Arrange
      userDetailsDTO = authService.getMe("john@example.com");

      // Assert
      assertNotNull(userDetailsDTO);
      assertEquals(validUser.getId(), userDetailsDTO.id());
      assertEquals(validUser.getName(), userDetailsDTO.username());
      assertEquals(validUser.getEmail(), userDetailsDTO.email());
      assertEquals(validUser.getEnabled(), userDetailsDTO.enable());

      // Verificar método findByEmailIgnoreCase foi chamado
      verify(userRepository, times(1)).findByEmailIgnoreCase("john@example.com");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar buscar usuário com email inexistente")
    void testGetMeUserNotFound() {
      // Arrange
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> authService.getMe("nonexistent@example.com"))
              .isInstanceOf(NoSuchElementException.class);
    }
  }

  @Nested
  class RegisterUser {
    private RegisteredUserDTO registeredUserDTO;

    @Test
    @DisplayName("Deve registrar novo usuário e retornar RegisteredUserDTO com tokens")
    void testRegisterUserSuccess() {
      String encodedPassword = "encodedPassword";

      // Arrange
      when(userRepository.save(any(User.class))).thenReturn(validUser);
      when(tokenService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
      when(tokenService.generateRefreshToken(any(User.class))).thenReturn("refresh-token-456");
      when(userRepository.existsByEmail(registerUserDTO.email())).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);

      // Act
      registeredUserDTO = authService.registerUser(registerUserDTO);

      // Assert - Verificar estrutura do objeto retornado
      assertNotNull(registeredUserDTO);
      assertEquals(1L, registeredUserDTO.id());
      assertEquals("access-token-123", registeredUserDTO.tokenDTO().accessToken());
      assertEquals("refresh-token-456", registeredUserDTO.tokenDTO().refreshToken());
    }

    @Test
    @DisplayName("Deve lançar DuplicateKeyException ao tentar registrar usuário com email já existente")
    void testRegisterUserEmailAlreadyExists() {
      // Arrange
      when(userRepository.existsByEmail(anyString())).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> authService.registerUser(registerUserDTO))
              .isInstanceOf(DuplicateKeyException.class)
              .hasMessage("Email already exists");

      // Verificar que o save não foi chamado porque o email já existe
      verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve codificar a senha antes de salvar o usuário")
    void testRegisterUserPasswordEncoded() {
      // Arrange - Criar um DTO com senha conhecida
      when(userRepository.save(any(User.class))).thenReturn(validUser);
      when(passwordEncoder.encode(registerUserDTO.password())).thenReturn("encodedPasswordHash");

      // Act
      registeredUserDTO = authService.registerUser(registerUserDTO);

      // Assert - Verificar que a senha original foi codificada
      verify(passwordEncoder, times(1)).encode(registerUserDTO.password());

      // Verificar o User salvo
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      User capturedUser = userCaptor.getValue();
      assertEquals("encodedPasswordHash", capturedUser.getPassword());
    }

    @Test
    @DisplayName("Deve criar usuário com enabled = true por padrão")
    void testRegisterUserEnabledDefault() {
      // Arrange - Senha codificada para teste
      when(passwordEncoder.encode(anyString())).thenReturn("encodedHash");
      when(userRepository.save(any(User.class))).thenReturn(validUser);

      // Act
      registeredUserDTO = authService.registerUser(registerUserDTO);

      // Assert - Verificar que o usuário foi criado com enabled = true
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      User capturedUser = userCaptor.getValue();
      assertTrue(capturedUser.getEnabled());
    }

    @Test
    @DisplayName("Deve salvar usuário com Profile.USER")
    void testRegisterUserProfile() {
      // Arrange - Senha codificada para teste
      when(passwordEncoder.encode(anyString())).thenReturn("encodedHash");
      when(userRepository.save(any(User.class))).thenReturn(validUser);

      // Act
      registeredUserDTO = authService.registerUser(registerUserDTO);

      // Assert - Verificar que o profile do usuário salvo é USER
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).save(userCaptor.capture());

      User capturedUser = userCaptor.getValue();
      assertEquals(Profile.USER, capturedUser.getProfile());
    }
  }
}