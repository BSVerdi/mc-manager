package br.com.mc_manager.server.dto.MinecraftManifest;

import java.util.List;

public record ManifestEntryDto(
        List<MinecraftVersionDto> versions
) {
}
