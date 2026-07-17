package br.com.mc_manager.server.dto;

public record ServerMetricsDTO(
        Double cpuUsagePercent,
        Long ramUsedMb,
        Long ramMaxMb,
        Long uptimeSeconds
) {
}
