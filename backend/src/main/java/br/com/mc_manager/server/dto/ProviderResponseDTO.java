package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.model.ServerType;

import java.util.List;

public record ProviderResponseDTO(
        ServerType provider,
        List<String> versions
) {
}
