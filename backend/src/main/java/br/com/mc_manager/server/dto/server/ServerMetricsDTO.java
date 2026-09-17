package br.com.mc_manager.server.dto.server;

public record ServerMetricsDTO(
        Long serverId,
        Double cpuUsagePercent,
        Long ramUsedMb,
        Long ramMaxMb,
        Long uptimeSeconds,
        Integer onlinePlayers
) {
}
