package br.com.mc_manager.server.dto.MinecraftManifest;

public record MinecraftVersionDetailsDto(String id, DownloadMinecraftVersionDto downloads, JavaVersionDto javaVersion) {
}
