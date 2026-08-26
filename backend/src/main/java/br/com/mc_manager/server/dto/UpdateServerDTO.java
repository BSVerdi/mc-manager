package br.com.mc_manager.server.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateServerDTO(
        @NotBlank String name,
        String minRam,
        String maxRam
) {
}
