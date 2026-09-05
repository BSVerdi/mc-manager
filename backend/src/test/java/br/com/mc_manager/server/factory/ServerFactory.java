package br.com.mc_manager.server.factory;


import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.User;

public class ServerFactory {

  public static Server createServerWithAllAttributes() {
    return Server.builder()
            .id(1L)
            .name("Example Server")
            .javaVersion("JAVA_21")
            .version("26.2")
            .port(25565)
            .build();
  }

  public static Server createServerWithAllAttributesWithOwner(User owner) {
    return Server.builder()
            .id(1L)
            .name("Example Server")
            .javaVersion("JAVA_21")
            .version("26.2")
            .port(25565)
            .owner(owner)
            .build();
  }

  public static Server createServerWithAllAttributesWithId(Long id) {
    return Server.builder()
            .id(id)
            .name("Example Server")
            .javaVersion("JAVA_21")
            .version("26.2")
            .port(25565)
            .build();
  }

}
