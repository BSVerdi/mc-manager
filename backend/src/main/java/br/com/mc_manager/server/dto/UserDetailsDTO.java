package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.User;

import java.time.Instant;

public record UserDetailsDTO(
        Long id,
        String username,
        String email,
        String profilePictureUrl,
        Boolean enable,
        Instant createdAt,
        Instant updatedAt
) {
  public UserDetailsDTO(User user) {
    this(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getProfilePictureUrl(),
            user.getEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
    );
  }
}
