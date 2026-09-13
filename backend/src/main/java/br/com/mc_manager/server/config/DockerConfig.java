package br.com.mc_manager.server.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DockerConfig {

  @Bean
  @Profile("docker")
  public DockerClient dockerClient() {
    DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix://var/run/docker.sock")
            .build();

    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(clientConfig.getDockerHost())
            .build();

    return DockerClientImpl.getInstance(clientConfig, httpClient);
  }
}
