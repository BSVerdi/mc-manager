package br.com.mc_manager.server.dto.server;

import jakarta.validation.constraints.NotBlank;

public record CreateServerDTO(
        @NotBlank
        String name,

        String minRam,
        String maxRam,

        @NotBlank
        String version
) {
}
