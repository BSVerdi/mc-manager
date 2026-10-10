package br.com.mc_manager.server.dto.minecraftManifest;

public record MinecraftVersionDetailsDTO(String id, DownloadMinecraftVersionDTO downloads, JavaVersionDTO javaVersion) {
}
