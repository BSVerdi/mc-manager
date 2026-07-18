package br.com.mc_manager.server.dto;

import java.util.List;

public record ServerMetricsDTO(
        Double cpuUsagePercent,
        Long ramUsedMb,
        Long ramMaxMb,
        Long uptimeSeconds,
        List<String> onlinePlayers
) {
}
