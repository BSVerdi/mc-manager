package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDTO;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import br.com.mc_manager.server.service.files.ServerFilesService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/files")
public class ServerFilesController {

  private final ServerFilesService serverFileService;

  public ServerFilesController(ServerFilesService serverFileService) {
    this.serverFileService = serverFileService;
  }

  @GetMapping
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<List<FileEntryDTO>> list(
          @PathVariable Long serverId,
          @RequestParam(defaultValue = "") String path
  ) throws IOException {
    var files = serverFileService.listServerFiles(serverId, path);

    return ResponseEntity.ok(files);
  }

  @GetMapping("/content")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<FileContentDTO> getContent(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    try {
      var content = serverFileService.getFileContent(serverId, path);

      return ResponseEntity.ok(content);
    } catch (IOException e) {
      throw new NoSuchFileException(e.getMessage());
    }
  }

  @PutMapping("/content")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> saveContent(@PathVariable Long serverId, @RequestParam String path, @RequestBody SaveFileRequestDTO request) throws IOException {
    serverFileService.saveFileContent(serverId, path, request);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/download")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Resource> download(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    MediaDTO media = serverFileService.downloadFile(serverId, path);

    String fileName = media.path().toString();
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

    return ResponseEntity.ok()
            .contentType(media.mediaType())
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
            .body(media.resource());
  }

  @GetMapping("/download-zip")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<StreamingResponseBody> downloadZip(@PathVariable Long serverId, @RequestParam String path) {
    StreamingResponseBody responseBody = out -> {
      serverFileService.downloadFolder(serverId, path, out);
    };

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/zip"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\"")
            .body(responseBody);
  }

  @PostMapping("/upload")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> upload(
          @PathVariable Long serverId,
          @RequestParam(defaultValue = "") String path,
          @RequestParam("file") MultipartFile file
  ) throws IOException {
    serverFileService.uploadFile(serverId, path, file);

    return ResponseEntity.noContent().build();
  }

  @PostMapping("/upload-zip")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> uploadZip(
          @PathVariable Long serverId,
          @RequestParam String path,
          @RequestParam("file") MultipartFile file
  ) throws IOException {
    serverFileService.uploadFolder(serverId, path, file);

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/delete")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> delete(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    serverFileService.delete(serverId, path);

    return ResponseEntity.noContent().build();
  }

}