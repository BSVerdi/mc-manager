package br.com.mc_manager.server.service.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PathSecurityService {

  @Value("${mcmanager.data_root_directory}")
  private String dataRootDirectory;

  public Path resolveSafePath(String scopeRelativePath, String path) {
    Path base = Paths.get(dataRootDirectory).normalize().toAbsolutePath();
    Path scope = base.resolve(scopeRelativePath).normalize();

    if (!scope.startsWith(base)) {
      throw new SecurityException("Caminho fora do diretório do servidor");
    }

    Path targetPath = scope.resolve(path).normalize();

    if (!targetPath.startsWith(scope)) {
      throw new SecurityException("Caminho fora do escopo permitido");
    }

    return targetPath;
  }

  public Path getFolderPath(String scope, String path) {
    Path folderPath = resolveSafePath(scope, path);

    if (!Files.isDirectory(folderPath)) {
      throw new IllegalArgumentException("O caminho informado não é um diretório");
    }

    return folderPath;
  }

  public Path getFilePath(String scope, String path) {
    Path filePath = resolveSafePath(scope, path);

    if (!Files.isRegularFile(filePath)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    return filePath;
  }

}
