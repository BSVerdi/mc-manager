package br.com.mc_manager.server.factory;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileDtoFactory {

  public static FileEntryDTO createFileEntryDTO(String name, long size) {
    return new FileEntryDTO(name, false, size, 0L, null);
  }

  public static FileEntryDTO createFolderEntryDTO(String name) {
    return new FileEntryDTO(name, true, 0L, 0L, null);
  }

  public static List<FileEntryDTO> createFileEntryList(int count) {
    List<FileEntryDTO> files = new ArrayList<>();

    for (int i = 1; i <= count; i++) {
      files.add(createFileEntryDTO("file" + i + ".txt", i * 1024));
    }

    return files;
  }

  public static FileContentDTO createFileContentDTO(String path, String content) {
    return new FileContentDTO(path, content, true, "txt");
  }

  public static FileContentDTO createNonEditableFileContentDTO(String path) {
    return new FileContentDTO(path, null, false, "txt");
  }

  public static MediaDto createMediaDTO(String fileName, byte[] content) {
    return new MediaDto(
            MediaType.TEXT_PLAIN,
            new ByteArrayResource(content),
            Path.of(fileName)
    );
  }

  public static MediaDto createMediaDTO(MediaType mediaType, String fileName, byte[] content) {
    return new MediaDto(
            mediaType,
            new ByteArrayResource(content),
            Path.of(fileName)
    );
  }
}
