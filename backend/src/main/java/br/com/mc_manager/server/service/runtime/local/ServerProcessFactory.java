package br.com.mc_manager.server.service.runtime.local;

import java.io.IOException;
import java.nio.file.Path;

public interface ServerProcessFactory {
  Process create(String javaExecutable, String minRam, String maxRam, Path serverDirectory) throws IOException;
}
