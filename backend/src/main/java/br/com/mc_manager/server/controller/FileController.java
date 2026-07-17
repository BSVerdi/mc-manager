package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import br.com.mc_manager.server.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

  @Autowired
  private FileService service;

  @GetMapping
  public ResponseEntity<List<FileEntryDTO>> list(@RequestParam(defaultValue = "") String path) throws IOException {
    List<FileEntryDTO> list = service.listFiles(path);

    return ResponseEntity.ok().body(list);
  }

  @GetMapping("/content")
  public ResponseEntity<FileContentDTO> getContent(@RequestParam String path) throws IOException {
    FileContentDTO dto = service.getContent(path);

    return ResponseEntity.ok(dto);
  }

  @PutMapping("/content")
  public ResponseEntity<Void> saveContent(@RequestParam String path, @RequestBody SaveFileRequestDTO request) throws IOException {
    service.saveContent(path, request);

    return ResponseEntity.noContent().build();
  }

}