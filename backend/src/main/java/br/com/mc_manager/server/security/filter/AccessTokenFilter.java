package br.com.mc_manager.server.security.filter;

import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
      var email = service.verifyToken(token);
      var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

      var authentication = new UsernamePasswordAuthenticationToken(
              email,
              null,
              user.getAuthorities()
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String recoverRequestToken(HttpServletRequest request) {
    var authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader != null) {
      return authorizationHeader.replace("Bearer ", "");
    }

    return null;
  }
}
