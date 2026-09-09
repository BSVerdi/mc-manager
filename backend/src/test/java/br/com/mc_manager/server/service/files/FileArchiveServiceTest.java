package br.com.mc_manager.server.service.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileArchiveServiceTest {

  @Mock
  private PathSecurityService pathSecurityService;

  @Mock
  private MultipartFile mockZipFile;

  @InjectMocks
  private FileArchiveService fileArchiveService;

  @TempDir
  private Path tempDir;

  private Path sourceDirectory;
  private Path targetDirectory;

  @BeforeEach
  void setUp() throws IOException {
    sourceDirectory = tempDir.resolve("source");
    targetDirectory = tempDir.resolve("target");

    createDirectories(sourceDirectory);
    createDirectories(targetDirectory);
  }

  private byte[] createZip(String entryName, String content) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (ZipOutputStream zipOutput = new ZipOutputStream(output)) {
      ZipEntry entry = new ZipEntry(entryName);
      zipOutput.putNextEntry(entry);
      zipOutput.write(content.getBytes(StandardCharsets.UTF_8));

      zipOutput.closeEntry();
    }

    return output.toByteArray();
  }

  private List<String> readZipEntries(byte[] zipBytes) throws IOException {
    List<String> entries = new ArrayList<>();

    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;

      while ((entry = zipInput.getNextEntry()) != null) {
        entries.add(entry.getName());
      }
    }

    return entries;
  }

  private boolean isValidZip(byte[] bytes) {
    try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      while (zipInput.getNextEntry() != null) {
      }

      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Nested
  class DownloadFolder {

    @Test
    void deveCompactarPastaComSucesso() throws Exception {
      // Arrange
      Path file = sourceDirectory.resolve("arquivo.txt");

      Files.writeString(
              file,
              "conteudo do arquivo",
              StandardCharsets.UTF_8
      );

      when(pathSecurityService.getFolderPath("scopeValido", "source"))
              .thenReturn(sourceDirectory);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      fileArchiveService.downloadFolder("scopeValido", "source", outputStream);

      // Assert
      byte[] zipBytes = outputStream.toByteArray();

      assertThat(zipBytes).isNotEmpty();

      try (ZipInputStream zipInputStream = new ZipInputStream(
              new ByteArrayInputStream(zipBytes)
      )) {
        ZipEntry entry = zipInputStream.getNextEntry();

        assertThat(entry).isNotNull();
        assertThat(entry.getName()).isEqualTo("arquivo.txt");

        String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(content).isEqualTo("conteudo do arquivo");
      }

      verify(pathSecurityService).getFolderPath("scopeValido", "source");
    }

    @Test
    void deveCompactarArquivosDeSubdiretorios() throws Exception {
      // Arrange
      Path subDirectory = sourceDirectory.resolve("subpasta");
      createDirectories(subDirectory);

      Files.writeString(sourceDirectory.resolve("arquivo.txt"), "arquivo principal");
      Files.writeString(subDirectory.resolve("outro.txt"), "arquivo interno");

      when(pathSecurityService.getFolderPath("scopeValido", "source"))
              .thenReturn(sourceDirectory);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      fileArchiveService.downloadFolder("scopeValido", "source", outputStream);

      // Assert
      List<String> entries = readZipEntries(outputStream.toByteArray());

      assertThat(entries).contains("arquivo.txt", "subpasta/outro.txt");
    }

    @Test
    void deveCompactarPastaVazia() throws Exception {
      // Arrange
      when(pathSecurityService.getFolderPath("scopeValido", "source"))
              .thenReturn(sourceDirectory);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      // Act
      fileArchiveService.downloadFolder("scopeValido", "source", outputStream);

      // Assert
      assertThat(outputStream.toByteArray()).isNotEmpty();
      assertThat(isValidZip(outputStream.toByteArray())).isTrue();
    }

    @Test
    void devePropagarSecurityExceptionDoPathSecurityService() throws IOException {
      // Arrange
      when(pathSecurityService.getFolderPath("scopeInvalido", "source"))
              .thenThrow(new SecurityException("Acesso negado"));

      OutputStream outputStream = mock(OutputStream.class);

      // Act & Assert
      assertThatThrownBy(
              () -> fileArchiveService.downloadFolder(
                      "scopeInvalido",
                      "source",
                      outputStream)
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Acesso negado");

      verify(outputStream, never()).write(any(byte[].class));
    }

    @Test
    void devePropagarErroQuandoPastaNaoPodeSerResolvida() throws Exception {
      // Arrange
      when(pathSecurityService.getFolderPath("scope", "source"))
              .thenThrow(new IllegalArgumentException("O caminho informado não é um diretório"));

      OutputStream outputStream = mock(OutputStream.class);

      // Act & Assert
      assertThatThrownBy(
              () -> fileArchiveService.downloadFolder(
                      "scope",
                      "source",
                      outputStream
              )
      )
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("não é um diretório");
    }
  }

  @Nested
  class UploadFolder {

    @Test
    void deveDescompactarArquivoComSucesso() throws Exception {
      // Arrange
      byte[] zipBytes = createZip("arquivo.txt", "conteudo do arquivo");

      when(pathSecurityService.getFolderPath("scopeValido", "target"))
              .thenReturn(targetDirectory);

      when(mockZipFile.getInputStream()).thenReturn(new ByteArrayInputStream(zipBytes));

      // Act
      fileArchiveService.uploadFolder("scopeValido", "target", mockZipFile);

      // Assert
      Path extractedFile = targetDirectory.resolve("arquivo.txt");

      assertThat(extractedFile).exists();
      assertThat(Files.readString(extractedFile)).isEqualTo("conteudo do arquivo");

      verify(mockZipFile).getInputStream();
      verify(pathSecurityService).getFolderPath("scopeValido", "target");
    }

    @Test
    void deveDescompactarArquivosDeSubdiretorios() throws Exception {
      // Arrange
      byte[] zipBytes = createZip("subpasta/arquivo.txt", "conteudo interno");

      when(pathSecurityService.getFolderPath("scopeValido", "target"))
              .thenReturn(targetDirectory);

      when(mockZipFile.getInputStream()).thenReturn(new ByteArrayInputStream(zipBytes));

      // Act
      fileArchiveService.uploadFolder("scopeValido", "target", mockZipFile);

      // Assert
      Path extractedFile = targetDirectory.resolve("subpasta").resolve("arquivo.txt");

      assertThat(extractedFile).exists();
      assertThat(Files.readString(extractedFile)).isEqualTo("conteudo interno");
    }

    @Test
    void devePropagarIOExceptionAoLerArquivo() throws Exception {
      // Arrange
      when(pathSecurityService.getFolderPath("scopeValido", "target"))
              .thenReturn(targetDirectory);

      when(mockZipFile.getInputStream()).thenThrow(new IOException("Erro ao ler arquivo"));

      // Act & Assert
      assertThatThrownBy(
              () -> fileArchiveService.uploadFolder(
                      "scopeValido",
                      "target",
                      mockZipFile
              )
      )
              .isInstanceOf(IOException.class)
              .hasMessage("Erro ao ler arquivo");
    }

    @Test
    void devePropagarSecurityExceptionDoPathSecurityService() throws IOException {
      // Arrange
      when(pathSecurityService.getFolderPath("scopeInvalido", "target"))
              .thenThrow(new SecurityException("Acesso negado"));

      // Act & Assert
      assertThatThrownBy(() ->
              fileArchiveService.uploadFolder(
                      "scopeInvalido",
                      "target",
                      mockZipFile
              )
      )
              .isInstanceOf(SecurityException.class)
              .hasMessage("Acesso negado");

      verify(mockZipFile, never())
              .getInputStream();
    }

    @Test
    void deveRejeitarEntradaZipQueTentaSairDoDiretorioAlvo()
            throws Exception {

      // Arrange
      byte[] maliciousZip = createZip("../arquivo-malicioso.txt", "conteudo malicioso");

      when(pathSecurityService.getFolderPath("scopeValido", "target"))
              .thenReturn(targetDirectory);

      when(mockZipFile.getInputStream())
              .thenReturn(new ByteArrayInputStream(maliciousZip));

      // Act & Assert
      assertThatThrownBy(
              () -> fileArchiveService.uploadFolder(
                      "scopeValido",
                      "target",
                      mockZipFile
              )
      ).isInstanceOf(SecurityException.class);

      assertThat(tempDir.resolve("arquivo-malicioso.txt")).doesNotExist();
    }

    @Test
    void deveRejeitarEntradaZipComTraversalProfundo()
            throws Exception {

      // Arrange
      byte[] maliciousZip = createZip(
              "pasta/../../../../arquivo-malicioso.txt",
              "conteudo malicioso"
      );

      when(pathSecurityService.getFolderPath("scopeValido", "target"))
              .thenReturn(targetDirectory);

      when(mockZipFile.getInputStream()).thenReturn(new ByteArrayInputStream(maliciousZip));

      // Act & Assert
      assertThatThrownBy(
              () -> fileArchiveService.uploadFolder(
                      "scopeValido",
                      "target",
                      mockZipFile
              )
      ).isInstanceOf(SecurityException.class);

      assertThat(tempDir.resolve("arquivo-malicioso.txt")).doesNotExist();
    }
  }
}
