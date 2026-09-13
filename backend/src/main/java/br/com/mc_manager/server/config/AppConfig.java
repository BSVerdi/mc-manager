package br.com.mc_manager.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import oshi.SystemInfo;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

  @Bean
  public SystemInfo systemInfo() {
    return new SystemInfo();
  }

  @Bean
  public ExecutorService executor() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public RestClient restClient() {
    return RestClient.create();
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
