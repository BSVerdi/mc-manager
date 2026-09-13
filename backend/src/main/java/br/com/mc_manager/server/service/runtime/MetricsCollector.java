package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.dto.ServerMetricsDTO;

public interface MetricsCollector {
  ServerMetricsDTO collect();
}
