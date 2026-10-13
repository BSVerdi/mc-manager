package br.com.mc_manager.server.service.files;

import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDTO;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static br.com.mc_manager.server.service.files.util.FileNameUtils.extractExtension;
import static java.nio.file.Files.createDirectories;

@Service
public class FileStorageService {

  private final PathSecurityService pathSecurityService;

  public FileStorageService(PathSecurityService pathSecurityService) {
    this.pathSecurityService = pathSecurityService;
  }

  public void createFolder(String scope, String targetPath) {
    Path path = pathSecurityService.resolveSafePath(scope, targetPath);

    try {
      createDirectories(path);
    } catch (IOException e) {
      throw new RuntimeException("Erro ao criar pasta.", e);
    }
  }

  public void createFile(String scope, String fileName) {
    Path path = pathSecurityService.resolveSafePath(scope, fileName);

    try {
      Files.createFile(path);
    } catch (IOException e) {
      throw new RuntimeException("Erro ao criar arquivo.", e);
    }
  }

  public void saveFile(String scope, String targetPath, MultipartFile file) throws IOException {
    Path filePath = pathSecurityService.resolveSafePath(scope, targetPath);

    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  public void saveFile(Path filePath, MultipartFile file) throws IOException {
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  public void saveFile(String scope, String targetPath, InputStream in) throws IOException {
    Path filePath = pathSecurityService.resolveSafePath(scope, targetPath);

    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  public void delete(String scope, String targetPath) throws IOException {
    Path target = pathSecurityService.resolveSafePath(scope, targetPath);

    if (!Files.exists(target)) return;

    if (Files.isDirectory(target)) {
      deleteFolder(scope, targetPath);
      return;
    }

    deleteFile(target);
  }

  public void deleteFile(String scope, String targetFilePath) throws IOException {
    Path filePath = pathSecurityService.resolveSafePath(scope, targetFilePath);

    Files.deleteIfExists(filePath);
  }

  public void deleteFile(Path filePath) throws IOException {
    Files.deleteIfExists(filePath);
  }

  public void deleteFolder(String scope, String targetPath) throws IOException {
    Path folderPath = pathSecurityService.getFolderPath(scope, targetPath);

    try (var walk = Files.walk(folderPath)) {
      walk.sorted(Comparator.reverseOrder())
              .forEach(path -> {
                try {
                  deleteFile(path);
                } catch (IOException e) {
                  throw new RuntimeException("Erro ao deletar " + path, e);
                }
              });
    }
  }

  public List<FileEntryDTO> listFiles(String targetFolderPath, String path) throws IOException {
    Path folderPath = pathSecurityService.getFolderPath(targetFolderPath, path);

    try (Stream<Path> walk = Files.list(folderPath)) {
      var files = walk.map(this::toFileEntryDTO)
              .sorted(Comparator.comparing(FileEntryDTO::directory).reversed()
                      .thenComparing(FileEntryDTO::name))
              .toList();

      return files;
    }
  }

  public MediaDTO downloadFile(String scope, String targetFilePath) throws IOException {
    Path filePath = pathSecurityService.getFilePath(scope, targetFilePath);

    Resource resource;
    try {
      resource = new UrlResource(filePath.toUri());
    } catch (MalformedURLException e) {
      throw new IOException("Não foi possível ler o arquivo " + filePath, e);
    }

    String contentType = Files.probeContentType(filePath);
    MediaType mediaType = contentType != null ?
            MediaType.parseMediaType(contentType) :
            MediaType.APPLICATION_OCTET_STREAM;

    return new MediaDTO(mediaType, resource, filePath.getFileName());
  }

  public void uploadFile(String scope, String targetFilePath, MultipartFile file) throws IOException {
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("Nome de arquivo inválido.");
    }

    String safeName = Paths.get(originalFilename).getFileName().toString();

    Path folder = pathSecurityService.getFolderPath(scope, targetFilePath);
    Path filePath = folder.resolve(safeName);

    saveFile(filePath, file);
  }

  private FileEntryDTO toFileEntryDTO(Path path) {
    String name = path.getFileName().toString();
    boolean isDirectory = Files.isDirectory(path);

    try {
      return new FileEntryDTO(
              name,
              isDirectory,
              isDirectory ? 0 : Files.size(path),
              Files.getLastModifiedTime(path).toMillis(),
              isDirectory ? "" : extractExtension(name)
      );
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
