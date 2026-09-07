package br.com.mc_manager.server.security.filter;

import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static br.com.mc_manager.server.factory.UserFactory.createUserWithAllAttributes;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenFilterTest {

  @Mock
  private TokenService tokenService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private AccessTokenFilter filter;

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext(); // evita vazamento de estado entre testes
  }

  @Test
  void semCookies_continuaACadeiaSemAutenticar() throws Exception {
    // Arrange
    when(request.getCookies()).thenReturn(null);

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void cookiePresenteMasNaoEhAccessToken_continuaSemAutenticar() throws Exception {
    // Arrange
    when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("outro_cookie", "valor")});

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void tokenValido_autenticaEContinuaACadeia() throws Exception {
    // Arrange
    Cookie cookie = new Cookie("access_token", "token-valido");
    when(request.getCookies()).thenReturn(new Cookie[]{cookie});
    when(tokenService.verifyAccessToken("token-valido")).thenReturn("example@email.com");

    User user = createUserWithAllAttributes();
    when(userRepository.findByEmailIgnoreCase("example@email.com")).thenReturn(Optional.of(user));

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isEqualTo("example@email.com");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void tokenValidoMasUsuarioNaoExisteNoBanco_limpaContexto() throws Exception {
    // Arrange
    Cookie cookie = new Cookie("access_token", "token-valido");
    when(request.getCookies()).thenReturn(new Cookie[]{cookie});
    when(tokenService.verifyAccessToken("token-valido")).thenReturn("fantasma@example.com");
    when(userRepository.findByEmailIgnoreCase("fantasma@example.com")).thenReturn(Optional.empty());

    // Act
    filter.doFilterInternal(request, response, filterChain);

    // Assert
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}