package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDTO;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
import java.util.zip.ZipOutputStream;

@Service
public class FileService {

  @Value("${mcmanager.server-directory}")
  private String serverDirectory;

  private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
          "jar", "class", "exe", "dll", "so", "mca", "dat", "zip", "gz"
  );

  private static final long MAX_EDITABLE_SIZE = 2 * 1024 * 1024;

  public List<FileEntryDTO> listFiles(String path) {
    Path target = resolveSafePath(path);

    if (!Files.isDirectory(target)) {
      throw new IllegalArgumentException("O caminho informado não é um diretório.");
    }

    try (Stream<Path> stream = Files.list(target)) {
      return stream.map(this::toFileEntryDTO)
              .sorted(Comparator.comparing(FileEntryDTO::directory).reversed()
                      .thenComparing(FileEntryDTO::name))
              .toList();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public FileContentDTO getContent(String path) throws IOException {
    Path target = resolveSafePath(path);
    if (!Files.isRegularFile(target)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    String extension = extractExtension(target.getFileName().toString());

    if (!isEditable(target)) {
      return new FileContentDTO(path, "", false, extension);
    }

    String content = Files.readString(target, StandardCharsets.UTF_8);

    return new FileContentDTO(path, content, true, extension);
  }

  public void saveContent(String path, SaveFileRequestDTO dto) throws IOException {
    Path target = resolveSafePath(path);

    if (Files.exists(target) && !Files.isRegularFile(target)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    if (!isEditable(target)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo");
    }

    Files.writeString(target, dto.content(), StandardCharsets.UTF_8);
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

  private String extractExtension(String name) {
    int dotIndex = name.lastIndexOf('.');

    return dotIndex >= 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
  }

  private Path resolveSafePath(String path) {
    Path base = Paths.get(serverDirectory).normalize().toAbsolutePath();
    Path target = base.resolve(path).normalize();

    if (!target.startsWith(base)) {
      throw new SecurityException("Caminho fora do diretório do servidor");
    }

    return target;
  }

  public MediaDTO download(String path) throws IOException {
    Path target = resolveSafePath(path);

    if (!Files.isRegularFile(target)) {
      throw new IllegalArgumentException("O caminho informado não é um arquivo.");
    }

    Resource resource;

    try {
      resource = new UrlResource(target.toUri());
    } catch (MalformedURLException e) {
      throw new IOException("Não foi possível ler o arquivo " + path, e);
    }

    String contentType = Files.probeContentType(target);
    MediaType mediaType = contentType != null ?
            MediaType.parseMediaType(contentType) :
            MediaType.APPLICATION_OCTET_STREAM;

    return new MediaDTO(mediaType, resource, target.getFileName());
  }

  public void upload(String path, MultipartFile file) throws IOException {
    Path targetDir = resolveSafePath(path);

    if (!Files.isDirectory(targetDir)) {
      throw new IllegalArgumentException("O caminho informado não é um diretório");
    }

    String originalFilename = file.getOriginalFilename();

    if (originalFilename == null || originalFilename.isBlank()) {
      throw new IllegalArgumentException("Nome de arquivo inválido");
    }

    String safeName = Paths.get(originalFilename).getFileName().toString();
    Path targetFile = targetDir.resolve(safeName).normalize();

    if (!targetFile.startsWith(targetDir)) {
      throw new SecurityException("Nome de arquivo inválido: " + originalFilename);
    }

    file.transferTo(targetFile);
  }

  public void downloadZip(String path, HttpServletResponse response) throws IOException {
    Path target = resolveSafePath(path);

    if (!Files.isDirectory(target)) {
      throw new IllegalArgumentException("O caminho informado não é um diretório");
    }

    String zipName = target.getFileName().toString() + ".zip";

    response.setContentType("application/zip");
    response.setHeader("Content-Disposition", "attachment; filename=" + zipName);

    try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
      try (Stream<Path> walk = Files.walk(target)) {
        walk.filter(Files::isRegularFile).forEach(file -> {
          String entryName = target.relativize(file).toString().replace('\\', '/');

          try {
            zipOut.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zipOut);
            zipOut.closeEntry();
          } catch (IOException e) {
            throw new RuntimeException("Erro ao compactar " + entryName, e);
          }
        });
      }

      zipOut.finish();
    }
  }
}
