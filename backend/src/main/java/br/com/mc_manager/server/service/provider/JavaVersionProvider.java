package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.service.provider.VanillaProvider.VersionJson;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JavaVersionProvider {

  private static final List<Integer> AVAILABLE = List.of(8, 17, 21, 25);

  private final VanillaProvider vanillaProvider;

  public JavaVersionProvider(VanillaProvider vanillaProvider) {
    this.vanillaProvider = vanillaProvider;
  }

  public String getJavaVersion(String version) {
    VersionJson json = vanillaProvider.getVersionJson(version);
    String javaVersion = resolveJavaVersion(json.javaVersion().majorVersion());
    return javaVersion;
  }

  private String resolveJavaVersion(String requiredVersion) {
    int requiredNumber = Integer.parseInt(requiredVersion);

    return AVAILABLE.stream()
            .filter(v -> v >= requiredNumber)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Sem java disponível para " + requiredVersion))
            .toString();
  }

}
