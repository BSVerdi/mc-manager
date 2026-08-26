package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDTO;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import br.com.mc_manager.server.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/files")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @GetMapping
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<List<FileEntryDTO>> list(
          @PathVariable Long serverId,
          @RequestParam(defaultValue = "") String path
  ) {
    var files = fileService.listFiles(serverId, path);

    return ResponseEntity.ok(files);
  }

  @GetMapping("/content")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<FileContentDTO> getContent(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    var content = fileService.getContent(serverId, path);

    return ResponseEntity.ok(content);
  }

  @PutMapping("/content")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> saveContent(@PathVariable Long serverId, @RequestParam String path, @RequestBody SaveFileRequestDTO request) throws IOException {
    fileService.saveContent(serverId, path, request);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/download")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Resource> download(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    MediaDTO media = fileService.download(serverId, path);

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
  public void downloadZip(@PathVariable Long serverId, @RequestParam String path, HttpServletResponse response
  ) throws IOException {
    fileService.downloadZip(serverId, path, response);
  }

  @PostMapping("/upload")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> upload(
          @PathVariable Long serverId,
          @RequestParam(defaultValue = "") String path,
          @RequestParam("file") MultipartFile file
  ) throws IOException {
    fileService.upload(serverId, path, file);

    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/delete")
  @PreAuthorize("@serverSecurity.isOwner(#serverId, authentication)")
  public ResponseEntity<Void> delete(@PathVariable Long serverId, @RequestParam String path) throws IOException {
    fileService.delete(serverId, path);

    return ResponseEntity.noContent().build();
  }

}