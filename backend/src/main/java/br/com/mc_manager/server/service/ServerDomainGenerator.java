package br.com.mc_manager.server.service;

import br.com.mc_manager.server.model.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerDomainGenerator {

  @Value("${mcmanager.base_domain}")
  private String baseDomain;

  public String generate(Server server) {
    String slug = server.getName()
            .toLowerCase()
            .replaceAll("[^a-zA-Z0-9]", "")
            .replaceAll("-|-$", "");

    String serverDomain = slug + "-" + server.getId().toString() + "." + baseDomain;
    return serverDomain;
  }
}
