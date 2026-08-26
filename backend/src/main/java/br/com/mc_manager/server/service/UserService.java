package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class UserService implements UserDetailsService {

  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
  }

  public User currentUser(Principal principal) {
    return repository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
  }

  public User currentUser(String username) {
    return repository.findByEmailIgnoreCase(username).orElseThrow();
  }

}
