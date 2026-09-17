package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.server.ServerMetricsDTO;

public interface MetricsCollector {
  ServerMetricsDTO collect();
}
