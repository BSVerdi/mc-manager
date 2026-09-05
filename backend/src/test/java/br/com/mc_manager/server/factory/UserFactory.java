package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.model.Profile;
import br.com.mc_manager.server.model.User;

public class UserFactory {

  public static User createUserWithAllAttributes() {
    return User.builder()
            .id(1L)
            .name("example")
            .email("example@email.com")
            .password("encodedPassword")
            .profilePictureUrl("profilePictureUrl")
            .profile(Profile.USER)
            .enabled(true)
            .build();
  }

}
