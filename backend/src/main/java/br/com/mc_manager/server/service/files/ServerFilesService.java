package br.com.mc_manager.server.service.files;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDTO;
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

  private final FileStorageService fileStorageService;
  private final FileArchiveService fileArchiveService;
  private final FileContentService fileContentService;
  private final PathSecurityService pathSecurityService;

  public ServerFilesService(
          FileStorageService fileStorageService,
          FileArchiveService fileArchiveService,
          FileContentService fileContentService,
          PathSecurityService pathSecurityService
  ) {
    this.fileStorageService = fileStorageService;
    this.fileArchiveService = fileArchiveService;
    this.fileContentService = fileContentService;
    this.pathSecurityService = pathSecurityService;
  }

  public void createBaseServerFiles(Long serverId, Integer serverPort) throws IOException {
    String serverScope = "servers/";

    fileStorageService.createFolder(serverScope, serverId.toString());

    writeEula(serverId);
    writeServerProperties(serverId, serverPort);
  }


  public void deleteServerFiles(Long serverId) throws IOException {
    fileStorageService.deleteFolder("servers/" + serverId + "/", "");
  }

  private void writeServerProperties(Long serverId, Integer port) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.createFile(serverScope, "server.properties");
    fileContentService.saveFileContent(
            serverScope,
            "server.properties",
            "server-port=" + port + "\nmotd=Servidor gerenciado pelo mc-manager\n"
    );
  }

  private void writeEula(Long serverId) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.createFile(serverScope, "eula.txt");
    fileContentService.saveFileContent(
            serverScope,
            "eula.txt",
            "# Gerado automaticamente pelo mc-manager\neula=true\n"
    );
  }

  public void downloadServerJar(Long serverId, InputStream inputStream) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.saveFile(serverScope, "server.jar", inputStream);
  }

  public List<FileEntryDTO> listServerFiles(Long serverId, String path) throws IOException {
    String scope = "servers/" + serverId + "/";

    List<FileEntryDTO> serverFiles = fileStorageService.listFiles(scope, path);

    return serverFiles;
  }


  public FileContentDTO getFileContent(Long serverId, String path) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    FileContentDTO fileContent = fileContentService.getFileContent(serverScope, path);

    return fileContent;
  }


  public void saveFileContent(Long serverId, String path, SaveFileRequestDTO request) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileContentService.saveFileContent(serverScope, path, request.content());
  }

  public MediaDTO downloadFile(Long serverId, String path) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    MediaDTO downloadFile = fileStorageService.downloadFile(serverScope, path);

    return downloadFile;
  }

  public void downloadFolder(Long serverId, String path, OutputStream outputStream) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileArchiveService.downloadFolder(serverScope, path, outputStream);
  }

  public void uploadFile(Long serverId, String path, MultipartFile file) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileStorageService.uploadFile(serverScope, path, file);
  }


  public void uploadFolder(Long serverId, String path, MultipartFile file) throws IOException {
    String serverScope = "servers/" + serverId + "/";

    fileArchiveService.uploadFolder(serverScope, path, file);
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
