package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.LoginDTO;
import br.com.mc_manager.server.dto.TokenDTO;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

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

  public TokenDTO refreshToken(@Valid String refreshToken) {
    var userId = Long.valueOf(tokenService.verifyToken(refreshToken));
    var user = userRepository.findById(userId).orElseThrow();

    var accessToken = tokenService.generateToken(user);
    var newRefreshToken = tokenService.generateRefreshToken(user);

    return new TokenDTO(accessToken, refreshToken);
  }

  public UserDetailsDTO getMe(String name) {
    var user = userRepository.findByEmailIgnoreCase(name).orElseThrow();

    return new UserDetailsDTO(user);
  }
}
