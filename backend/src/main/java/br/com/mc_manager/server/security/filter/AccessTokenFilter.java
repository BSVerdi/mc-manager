package br.com.mc_manager.server.security.filter;

import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {

  @Autowired
  private TokenService service;

  @Autowired
  private UserRepository userRepository;

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain
  ) throws ServletException, IOException {
    String token = recoverRequestToken(request);

    if (token != null) {
      try {
        var email = service.verifyAccessToken(token);
        var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                user.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (Exception e) {
        SecurityContextHolder.clearContext();

        return;
      }

    }

    filterChain.doFilter(request, response);
  }

  private String recoverRequestToken(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }

    return Arrays.stream(request.getCookies())
            .filter(cookie -> "access_token".equals(cookie.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

}
