package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.*;
import br.com.mc_manager.server.model.Profile;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import br.com.mc_manager.server.security.service.TokenService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(AuthenticationManager authenticationManager, TokenService tokenService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.tokenService = tokenService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public TokenDTO login(LoginDTO dto) {
    var authenticationToken = new UsernamePasswordAuthenticationToken(
            dto.email(),
            dto.password()
    );

    var authentication = authenticationManager.authenticate(authenticationToken);

    var user = (User) authentication.getPrincipal();

    var accessToken = tokenService.generateAccessToken(user);
    var refreshToken = tokenService.generateRefreshToken(user);

    return new TokenDTO(accessToken, refreshToken);

  }

  @Transactional
  public TokenDTO refreshToken(@Valid String refreshToken) {
    var userEmail = tokenService.verifyRefreshToken(refreshToken);
    var user = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow();

    var accessToken = tokenService.generateAccessToken(user);
    var newRefreshToken = tokenService.generateRefreshToken(user);

    return new TokenDTO(accessToken, newRefreshToken);
  }

  public UserDetailsDTO getMe(String name) {
    var user = userRepository.findByEmailIgnoreCase(name).orElseThrow();

    return new UserDetailsDTO(user);
  }

  @Transactional
  public RegisteredUserDTO registerUser(RegisterUserDTO registerUser) {
    var exists = userRepository.existsByEmail(registerUser.email());

    if (exists) {
      throw new DuplicateKeyException("Email already exists");
    }

    var encodedPassword = passwordEncoder.encode(registerUser.password());

    var user = userRepository.save(new User(
            registerUser.name(),
            registerUser.email(),
            encodedPassword,
            Profile.USER
    ));

    var accessToken = tokenService.generateAccessToken(user);
    var refreshToken = tokenService.generateRefreshToken(user);

    return new RegisteredUserDTO(user.getId(), new TokenDTO(accessToken, refreshToken));
  }

}
