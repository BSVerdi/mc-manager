package br.com.mc_manager.server.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

import java.nio.file.Path;

public record MediaDTO(
        MediaType mediaType,
        Resource resource,
        Path path
) {
}
