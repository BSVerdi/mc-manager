package br.com.mc_manager.server.dto;

import br.com.mc_manager.server.service.runtime.ServerStatus;

public record StatusResponseDTO(ServerStatus status) {
}
