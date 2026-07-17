package br.com.mc_manager.server.dto;

public record FileEntryDTO(
        String name,
        boolean directory,
        long sizeBytes,
        long lastModified,
        String extension) {
}
