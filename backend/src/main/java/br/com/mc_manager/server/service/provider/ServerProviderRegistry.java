package br.com.mc_manager.server.service.provider;

import br.com.mc_manager.server.dto.ProviderResponseDTO;
import br.com.mc_manager.server.model.ServerType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServerProviderRegistry {

  private final Map<ServerType, MinecraftProvider> providers;

  public ServerProviderRegistry(List<MinecraftProvider> providers) {
    this.providers = providers.stream().collect(Collectors.toMap(MinecraftProvider::type, Function.identity()));
  }

  public MinecraftProvider get(ServerType type) {
    MinecraftProvider provider = providers.get(type);
    if (provider == null) throw new IllegalArgumentException("Tipo desconhecido: " + type);
    return provider;
  }

  public List<ProviderResponseDTO> getProviderCatalog() {
    List<ServerType> types = this.providers.keySet().stream().toList();

    return types.stream()
            .map(t -> new ProviderResponseDTO(t, providers.get(t).getReleasedVersions()))
            .toList();
  }
}
