package br.com.mc_manager.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServerDTO(
        @NotBlank
        String name,


        @NotNull
        @Min(value = 1024)
        @Max(value = 65535)
        Integer port,

        String minRam,
        String maxRam,

        @NotBlank
        String version
) {
}
