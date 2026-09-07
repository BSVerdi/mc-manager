package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDto;
import br.com.mc_manager.server.dto.SaveFileRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

@Service
public class ServerFilesService {

  private final MinecraftMetaDataService minecraftMetaDataService;
  private final FileStorageService fileStorageService;
  private final PathSecurityService pathSecurityService;

  public ServerFilesService(MinecraftMetaDataService minecraftMetaDataService, FileStorageService fileStorageService, PathSecurityService pathSecurityService) {
    this.minecraftMetaDataService = minecraftMetaDataService;
    this.fileStorageService = fileStorageService;
    this.pathSecurityService = pathSecurityService;
  }

  public void createBaseServerFiles(Long serverId, Integer serverPort) throws IOException {
    String serverScope = "servers/";

    fileStorageService.createFolder(serverScope, serverId.toString());

    writeEula(serverId);
    writeServerProperties(serverId, serverPort);
  }


  public void deleteServerFiles(Long serverId) throws IOException {
    fileStorageService.deleteFolder("servers/" + serverId, "/");
  }

  private void writeServerProperties(Long serverId, Integer port) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.createFile(serverScope, "server.properties");
    fileStorageService.saveFileContent(
            serverScope,
            "server.properties",
            "server-port=" + port + "\nmotd=Servidor gerenciado pelo mc-manager\n"
    );
  }

  private void writeEula(Long serverId) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.createFile(serverScope, "eula.txt");
    fileStorageService.saveFileContent(
            serverScope,
            "eula.txt",
            "# Gerado automaticamente pelo mc-manager\neula=true\n"
    );
  }

  public String downloadVanillaJar(Long serverId, String serverVersion) throws IOException {
    var versionDetails = minecraftMetaDataService.getMinecraftServerVersionDetails(serverVersion);
    var javaVersion = versionDetails.javaVersion().majorVersion();
    var jarUrl = versionDetails.downloads().server().url();


    try (InputStream in = minecraftMetaDataService.getServerInputStream(jarUrl);) {
      String serverScope = "servers/" + serverId + "/";

      fileStorageService.saveFile(serverScope, "server.jar", in);
    }

    return javaVersion;
  }

  public List<FileEntryDTO> listServerFiles(Long serverId, String path) throws IOException {
    String scope = "servers/" + serverId + "/";

    List<FileEntryDTO> serverFiles = fileStorageService.listFiles(scope, path);

    return serverFiles;
  }


  public FileContentDTO getFileContent(Long serverId, String path) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    FileContentDTO fileContent = fileStorageService.getFileContent(serverScope, path);

    return fileContent;
  }


  public void saveFileContent(Long serverId, String path, SaveFileRequestDTO request) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.saveFileContent(serverScope, path, request.content());
  }

  public MediaDto downloadFile(Long serverId, String path) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    MediaDto downloadFile = fileStorageService.downloadFile(serverScope, path);

    return downloadFile;
  }


  public void downloadFolder(Long serverId, String path, OutputStream outputStream) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.downloadFolder(serverScope, path, outputStream);
  }

  public void uploadFile(Long serverId, String path, MultipartFile file) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.uploadFile(serverScope, path, file);
  }


  public void uploadFolder(Long serverId, String path, MultipartFile file) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.uploadFolder(serverScope, path, file);
  }

  public void delete(Long serverId, String path) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.delete(serverScope, path);
  }

  public Path getServerDirectory(Long id) {
    String serverScope = "servers/" + id + "/";

    Path serverdirectory = pathSecurityService.resolveSafePath(serverScope, "");
    return serverdirectory;
  }

}
