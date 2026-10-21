package br.com.mc_manager.server.service.runtime.local;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
@Profile("!docker")
public class LocalServerProcessFactory {
  public Process create(String javaExecutable, String minRam, String maxRam, Path serverDirectory) throws IOException {
    ProcessBuilder pb = new ProcessBuilder(
            javaExecutable,
            "-Xms" + minRam,
            "-Xmx" + maxRam,
            "-jar", "server.jar",
            "nogui"
    );

    pb.directory(serverDirectory.toFile());
    pb.redirectErrorStream(true);

    return pb.start();
  }
}
