package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.LoginDTO;
import br.com.mc_manager.server.dto.RefreshTokenDTO;
import br.com.mc_manager.server.dto.TokenDTO;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private UserRepository userRepository;

  public TokenDTO login(LoginDTO dto) {
    var authenticationToken = new UsernamePasswordAuthenticationToken(
            dto.email(),
            dto.password()
    );

    var authentication = authenticationManager.authenticate(authenticationToken);

    var user = (User) authentication.getPrincipal();

    var accessToken = tokenService.generateToken(user);
    var refreshToken = tokenService.generateRefreshToken(user);

    return new TokenDTO(accessToken, refreshToken);

  }

  public TokenDTO refreshToken(@Valid RefreshTokenDTO dto) {
    var userId = Long.valueOf(tokenService.verifyToken(dto.refreshToken()));
    var user = userRepository.findById(userId).orElseThrow();

    var accessToken = tokenService.generateToken(user);
    var refreshToken = tokenService.generateRefreshToken(user);

    return new TokenDTO(accessToken, refreshToken);
  }
}
