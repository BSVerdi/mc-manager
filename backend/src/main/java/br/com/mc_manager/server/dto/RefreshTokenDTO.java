package br.com.mc_manager.server.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenDTO(
        @NotBlank String refreshToken
) {
}
