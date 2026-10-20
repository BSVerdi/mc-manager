package br.com.mc_manager.server.config;

import org.springframework.beans.factory.annotation.Value;
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
  public RestClient.Builder resclientBuilder(@Value("HTTP.USER-AGENT") String userAgent) {
    return RestClient.builder().defaultHeader("User-Agent", userAgent);
  }

  @Bean
  public RestClient mojangClient(RestClient.Builder builder) {
    return builder.clone().baseUrl("https://piston-meta.mojang.com").build();
  }

  @Bean
  public RestClient paperClient(RestClient.Builder builder) {
    return builder.clone().baseUrl("https://fill.papermc.io/v3").build();
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
