package br.com.mc_manager.server.dto;

public record FileContentDTO(
        String path,
        String content,
        boolean editable,
        String extension
) {
}
