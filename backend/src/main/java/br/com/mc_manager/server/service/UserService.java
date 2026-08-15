package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.RegisterUserDTO;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.model.Profile;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

  @Autowired
  private UserRepository repository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
  }

  @Transactional
  public UserDetailsDTO registerUser(RegisterUserDTO registerUser) {
    var exists = repository.existsByEmail(registerUser.email());

    if (exists) {
      throw new DuplicateKeyException("Email already exists");
    }

    var encodedPassword = passwordEncoder.encode(registerUser.password());

    var user = repository.save(new User(
            registerUser.name(),
            registerUser.email(),
            encodedPassword,
            Profile.USER
    ));

    return new UserDetailsDTO(user);
  }

}
