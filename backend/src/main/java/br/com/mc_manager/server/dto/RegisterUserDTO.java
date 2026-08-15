package br.com.mc_manager.server.dto;

public record RegisterUserDTO(
        String name,
        String email,
        String password
) {
}
