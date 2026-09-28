package br.com.mc_manager.server.service.files;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class FileArchiveService {

  private final PathSecurityService pathSecurityService;

  public FileArchiveService(PathSecurityService pathSecurityService) {
    this.pathSecurityService = pathSecurityService;
  }

  public void downloadFolder(String scope, String targetFolderPath, OutputStream outputStream) throws IOException {
    Path folderPath = pathSecurityService.getFolderPath(scope, targetFolderPath);

    ZipOutputStream zipOut = new ZipOutputStream(outputStream);
    try {
      zipFolder(folderPath, zipOut);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void uploadFolder(String scope, String targetFilePath, MultipartFile zipFile) throws IOException {
    Path folderPath = pathSecurityService.getFolderPath(scope, targetFilePath);

    try (ZipInputStream zipIn = new ZipInputStream(zipFile.getInputStream(), StandardCharsets.UTF_8)) {
      unzipFolder(folderPath, zipIn);
    }
  }

  private void zipFolder(Path folderPath, ZipOutputStream zipOut) throws IOException {
    try (Stream<Path> walk = Files.walk(folderPath)) {
      walk.filter(Files::isRegularFile)
              .sorted(Comparator.comparing(Path::toString))
              .forEach(file -> {
                String entryName = folderPath.relativize(file).toString().replace('\\', '/');

                try {
                  ZipEntry entry = new ZipEntry(entryName);
                  zipOut.putNextEntry(entry);
                  Files.copy(file, zipOut);
                  zipOut.closeEntry();
                } catch (IOException e) {
                  throw new RuntimeException("Erro ao compactar " + entryName, e);
                }
              });
    }

    zipOut.finish();
  }

  private void unzipFolder(Path folderPath, ZipInputStream zipIn) throws IOException {
    ZipEntry entry;

    while ((entry = zipIn.getNextEntry()) != null) {
      String entryName = entry.getName().replace('\\', '/');

      Path targetFilePath = folderPath.resolve(entryName).normalize();

      if (!targetFilePath.startsWith(folderPath)) {
        throw new SecurityException("Entrada de zip inválida: " + entryName);
      }

      if (entry.isDirectory()) {
        Files.createDirectories(targetFilePath);
      } else {
        Files.createDirectories(targetFilePath.getParent());
        Files.copy(zipIn, targetFilePath);
      }

      zipIn.closeEntry();
    }
  }
}
