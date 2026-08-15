package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.User;

public record UserDetailsDTO(
        Long id,
        String username,
        String email,
        Boolean enable
) {
  public UserDetailsDTO(User user) {
    this(user.getId(), user.getName(), user.getEmail(), user.getEnabled());
  }
}
