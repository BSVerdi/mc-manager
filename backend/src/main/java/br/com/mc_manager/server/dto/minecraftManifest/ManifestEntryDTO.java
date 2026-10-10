package br.com.mc_manager.server.dto.minecraftManifest;

import java.util.List;

public record ManifestEntryDTO(
        List<MinecraftVersionDTO> versions
) {
}
