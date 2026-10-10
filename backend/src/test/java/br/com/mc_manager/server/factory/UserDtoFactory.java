package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.UpdateUserDTO;
import br.com.mc_manager.server.dto.UserDetailsDTO;

import java.time.Instant;

public class UserDtoFactory {

  public static UserDetailsDTO createUserDetailsWithAllAttributes() {
    return new UserDetailsDTO(
            1L,
            "Example Name",
            "example@email.com",
            "Example Pic",
            true,
            Instant.now(),
            null
    );
  }

  ;

  public static UserDetailsDTO createUserDetailsWithAttributes(String name, String email) {
    return new UserDetailsDTO(
            1L,
            name,
            email,
            "Example Pic",
            true,
            Instant.now(),
            null
    );
  }

  public static UpdateUserDTO createUpdateUserDtoWithAllAttributes() {
    return new UpdateUserDTO("New Username");
  }

  public static UpdateUserDTO createUpdateUserDtoWithNullAttributes() {
    return new UpdateUserDTO(null);
  }

}
