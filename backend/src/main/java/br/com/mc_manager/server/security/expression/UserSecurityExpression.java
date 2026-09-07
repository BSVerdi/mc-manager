package br.com.mc_manager.server.security.expression;

import br.com.mc_manager.server.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurityExpression {

  private final UserService userService;

  public UserSecurityExpression(UserService userService) {
    this.userService = userService;
  }

  public boolean isUser(Long userId, Authentication authentication) {
    String username = authentication.getName();

    try {
      userService.assertIsUser(userId, username);

      return true;
    } catch (SecurityException e) {
      return false;
    }
  }
}
