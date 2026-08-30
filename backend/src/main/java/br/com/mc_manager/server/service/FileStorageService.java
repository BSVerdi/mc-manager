package br.com.mc_manager.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

  @Value("${mcmanager.data-root-directory}" + "/images")
  private String uploadPath;

  public String saveFile(MultipartFile file) throws IOException {
    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

    Path path = Paths.get(uploadPath).resolve(fileName);
    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

    return "images/ +" + fileName;
  }

}
