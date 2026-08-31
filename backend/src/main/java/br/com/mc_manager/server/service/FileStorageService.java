package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

  private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
          "jar", "class", "exe", "dll", "so", "mca", "dat", "zip", "gz"
  );
  private static final long MAX_EDITABLE_SIZE = 2 * 1024 * 1024;
  @Value("${mcmanager.data-root-directory}")
  private String dataRootDirectory;

  public FileService(ServerFilesService serverFileProvisioningService) {
  }

  public void saveFile(String targetPath, String fileName, MultipartFile multipartFile) throws IOException {
    Path folderPath = getFolderPath(targetPath);

  }

  public void deleteFile(Path filePath) throws IOException {
    Files.deleteIfExists(filePath);
  }

  public void deleteFolder(String targetPath) throws IOException {
    Path folderPath = getFolderPath(targetPath);

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

  public List<FileEntryDTO> listFiles(String targetFolderPath) throws IOException {
    Path folderPath = getFolderPath(targetFolderPath);

    try (Stream<Path> walk = Files.walk(folderPath)) {
      var files = walk.map(this::toFileEntryDTO)
              .sorted(Comparator.comparing(FileEntryDTO::directory).reversed()
                      .thenComparing(FileEntryDTO::name))
              .toList();

      return files;
    }
  }

  public FileContentDTO getFileContent(String targetFilePath) throws IOException {
    Path filePath = getFilePath(targetFilePath);

    String fileName = filePath.getFileName().toString();
    String extension = extractExtension(fileName);

    if (!isEditable(filePath)) {
      return new FileContentDTO(filePath.toString(), "", false, extension);
    }

    String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
    return new FileContentDTO(filePath.toString(), fileContent, true, extension);
  }

  public void saveFileContent(String targetFilePath, FileContentDTO fileContentDTO) throws IOException {
    Path filePath = getFilePath(targetFilePath);

    if (Files.exists(filePath) && !Files.isRegularFile(filePath)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    if (!isEditable(filePath)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo");
    }

    Files.writeString(filePath, fileContentDTO.content(), StandardCharsets.UTF_8);
  }

  public MediaDto downloadFile(String targetFilePath) throws IOException {
    Path filePath = getFilePath(targetFilePath);

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

    return new MediaDto(mediaType, resource, filePath.getFileName());
  }

  public void downloadFolder(String targetFolderPath, OutputStream outputStream) throws IOException {
    Path folderPath = getFolderPath(targetFolderPath);

    try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
      zipFolder(folderPath, zipOut);
    }
  }

  public void uploadFile(String targetFilePath, MultipartFile file) throws IOException {
    Path filePath = getFilePath(targetFilePath);

    String originalFilename = file.getOriginalFilename();

    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("Nome de arquivo inválido.");
    }

    String safeFileName = Paths.get(originalFilename).getFileName().toString();
    Path expectedFilePath = Paths.get(filePath.getParent().toString(), safeFileName);

    if (!targetFilePath.startsWith(dataRootDirectory)) {
      throw new SecurityException("Nome do arquivo inválido: " + originalFilename);
    }

    file.transferTo(expectedFilePath.toFile());
  }

  public void uploadFolder(String path, MultipartFile zipFile) throws IOException {
    Path folderPath = getFolderPath(path);

    try (ZipInputStream zipIn = new ZipInputStream(zipFile.getInputStream(), StandardCharsets.UTF_8)) {
      unzipFolder(folderPath, zipIn);
    }
  }

  private Path getFolderPath(String path) {
    Path folderPath = resolveSafePath(path);

    if (Files.isDirectory(folderPath)) {
      throw new IllegalArgumentException("O caminho informado não é um diretório");
    }

    return folderPath;
  }

  private Path getFilePath(String path) {
    Path filePath = resolveSafePath(path);

    if (!Files.isRegularFile(filePath)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    return filePath;
  }

  private void zipFolder(Path folderPath, ZipOutputStream zipOut) throws IOException {
    try (Stream<Path> walk = Files.walk(folderPath)) {
      walk.filter(Files::isRegularFile).forEach(file -> {
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

  private boolean isEditable(Path target) throws IOException {
    String extension = extractExtension(target.getFileName().toString());

    if (BLOCKED_EXTENSIONS.contains(extension)) {
      return false;
    }

    return (Files.exists(target) && Files.size(target) <= MAX_EDITABLE_SIZE);
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

  public String extractExtension(String name) {
    int dotIndex = name.lastIndexOf('.');

    return dotIndex >= 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
  }

  private Path resolveSafePath(String path) {
    Path base = Paths.get(dataRootDirectory).normalize().toAbsolutePath();
    Path targetPath = base.resolve(path).normalize();

    if (!targetPath.startsWith(base)) {
      throw new SecurityException("Caminho fora do diretório do servidor");
    }

    return targetPath;
  }

}
