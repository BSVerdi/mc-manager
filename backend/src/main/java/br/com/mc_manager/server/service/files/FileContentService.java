package br.com.mc_manager.server.service.files;

import br.com.mc_manager.server.dto.FileContentDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static br.com.mc_manager.server.service.files.util.FileNameUtils.extractExtension;
import static br.com.mc_manager.server.service.files.util.FileNameUtils.hasBlockedExtension;

@Service
public class FileContentService {

  private static final long MAX_EDITABLE_SIZE = 2 * 1024 * 1024;

  private final PathSecurityService pathSecurityService;

  public FileContentService(PathSecurityService pathSecurityService) {
    this.pathSecurityService = pathSecurityService;
  }

  public FileContentDTO getFileContent(String scope, String targetFilePath) throws IOException {
    Path filePath = pathSecurityService.getFilePath(scope, targetFilePath);

    String fileName = filePath.getFileName().toString();
    String extension = extractExtension(fileName);

    if (!isEditable(filePath)) {
      return new FileContentDTO(targetFilePath, "", false, extension);
    }

    String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
    return new FileContentDTO(targetFilePath, fileContent, true, extension);
  }

  public void saveFileContent(String scope, String targetFilePath, String content) throws IOException {
    Path filePath = pathSecurityService.getFilePath(scope, targetFilePath);

    if (!isEditable(filePath)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo");
    }

    Files.writeString(filePath, content, StandardCharsets.UTF_8);
  }

  private boolean isEditable(Path target) throws IOException {
    String extension = extractExtension(target.getFileName().toString());

    if (hasBlockedExtension(extension)) {
      return false;
    }

    return (Files.exists(target) && Files.size(target) <= MAX_EDITABLE_SIZE);
  }

}
