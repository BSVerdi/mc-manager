package br.com.mc_manager.server.dto.server;

import br.com.mc_manager.server.model.ServerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServerDTO(
        @NotBlank
        String name,

        String minRam,
        String maxRam,

        @NotBlank
        String version,

        @NotNull
        ServerType type
) {
}
