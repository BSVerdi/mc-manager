package br.com.mc_manager.server.security.expression;

import br.com.mc_manager.server.service.ServerManagementService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("serverSecurity")
public class ServerSecurityExpressions {

  private final ServerManagementService managementService;

  public ServerSecurityExpressions(ServerManagementService managementService) {
    this.managementService = managementService;
  }

  public boolean isOwner(Long serverId, Authentication authentication) {
    try {
      managementService.assertOwnership(serverId, authentication.getName());

      return true;
    } catch (SecurityException e) {
      return false;
    }
  }

  public boolean isOwner(Long serverId, String username) {
    try {
      managementService.assertOwnership(serverId, username);
      return true;
    } catch (SecurityException e) {
      return false;
    }
  }

}
