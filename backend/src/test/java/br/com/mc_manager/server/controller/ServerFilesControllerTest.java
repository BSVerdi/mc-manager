package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.FileContentDTO;
import br.com.mc_manager.server.dto.FileEntryDTO;
import br.com.mc_manager.server.dto.MediaDto;
import br.com.mc_manager.server.security.expression.ServerSecurityExpressions;
import br.com.mc_manager.server.service.ServerFilesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static br.com.mc_manager.server.factory.FileDtoFactory.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "mcmanager.data_root_directory=./target/test-data")
class ServerFilesControllerTest {

  private static final Long TEST_SERVER_ID = 1L;
  private static final String TEST_PATH = "/test/file.txt";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private ServerFilesService serverFilesService;

  @MockitoBean
  private ServerSecurityExpressions serverSecurityExpressions;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(get("/").with(csrf()))
            .build();

    given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(true);
  }

  @Nested
  @DisplayName("Listar Arquivos")
  class ListFilesTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - retorna arquivos do servidor")
    void sucesso() throws Exception {
      FileEntryDTO file1 = createFileEntryDTO("file1.txt", 1024);
      FileEntryDTO file2 = createFileEntryDTO("file2.txt", 2048);

      given(serverFilesService.listServerFiles(anyLong(), anyString()))
              .willReturn(List.of(file1, file2));

      mockMvc.perform(get("/api/servers/{serverId}/files", TEST_SERVER_ID))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.[0].name").value("file1.txt"))
              .andExpect(jsonPath("$.[1].sizeBytes").value(2048));
    }

    @Test
    @WithMockUser
    @DisplayName("Lista vazia quando não há arquivos")
    void listaVazia() throws Exception {
      given(serverFilesService.listServerFiles(anyLong(), anyString()))
              .willReturn(Collections.emptyList());

      mockMvc.perform(get("/api/servers/{serverId}/files", TEST_SERVER_ID))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Caminho vazio retorna arquivos da raiz")
    void caminhoVazio() throws Exception {
      List<FileEntryDTO> files = createFileEntryList(3);

      given(serverFilesService.listServerFiles(anyLong(), anyString()))
              .willReturn(files);

      mockMvc.perform(get("/api/servers/{serverId}/files?path=", TEST_SERVER_ID))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(3));
    }

    @Test
    @WithMockUser
    @DisplayName("Caminho com subdiretório é repassado ao service")
    void caminhoComSubdiretorio() throws Exception {
      List<FileEntryDTO> files = createFileEntryList(2);

      given(serverFilesService.listServerFiles(eq(TEST_SERVER_ID), eq("/subdir")))
              .willReturn(files);

      mockMvc.perform(get("/api/servers/{serverId}/files?path=/subdir", TEST_SERVER_ID))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.size()").value(2));
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(get("/api/servers/{serverId}/files", TEST_SERVER_ID))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(get("/api/servers/{serverId}/files", TEST_SERVER_ID))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Obter Conteúdo de Arquivo")
  class GetContentTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - retorna conteúdo do arquivo")
    void sucesso() throws Exception {
      String expectedContent = "Linha 1\nLinha 2\n";
      FileContentDTO fileContent = createFileContentDTO(TEST_PATH, expectedContent);

      given(serverFilesService.getFileContent(eq(TEST_SERVER_ID), eq(TEST_PATH)))
              .willReturn(fileContent);

      mockMvc.perform(get("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.content").value(expectedContent));
    }

    @Test
    @WithMockUser
    @DisplayName("Arquivo não encontrado retorna 404")
    void arquivoNaoEncontrado() throws Exception {
      given(serverFilesService.getFileContent(anyLong(), anyString()))
              .willThrow(new IOException("Arquivo não encontrado"));

      mockMvc.perform(get("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, "/nonexistent.txt"))
              .andExpect(status().isNotFound())
              .andExpect(content().string("Arquivo não encontrado"));
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(get("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(get("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Salvar Conteúdo de Arquivo")
  class SaveContentTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - salva conteúdo enviado")
    void sucesso() throws Exception {
      String content = """
                {
                  "content": "Example Content"
                }
              """;

      mockMvc.perform(put("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH)
                      .content(content)
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Falha ao salvar retorna erro de servidor")
    void falhaAoSalvar() throws Exception {
      String content = """
                {
                  "content": "Example Content"
                }
              """;

      doThrow(new IOException("Erro ao salvar"))
              .when(serverFilesService).saveFileContent(anyLong(), anyString(), any());

      mockMvc.perform(put("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH)
                      .content(content)
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      String content = """
                {
                  "content": "Example Content"
                }
              """;

      mockMvc.perform(put("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH)
                      .content(content)
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      String content = """
                {
                  "content": "Example Content"
                }
              """;

      mockMvc.perform(put("/api/servers/{serverId}/files/content?path={path}", TEST_SERVER_ID, TEST_PATH)
                      .content(content)
                      .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Download de Arquivo")
  class DownloadFileTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - retorna arquivo com Content-Disposition correto")
    void sucesso() throws Exception {
      String fileName = "\\arquivos\\documento.txt";
      String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
      byte[] fileContent = "Conteúdo do arquivo".getBytes(StandardCharsets.UTF_8);

      given(serverFilesService.downloadFile(anyLong(), anyString()))
              .willReturn(new MediaDto(MediaType.TEXT_PLAIN, new ByteArrayResource(fileContent), Path.of(fileName)));

      mockMvc.perform(get("/api/servers/{serverId}/files/download?path={path}", TEST_SERVER_ID, fileName)
                      .accept(MediaType.TEXT_PLAIN))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.TEXT_PLAIN))
              .andExpect(header().string(
                      HttpHeaders.CONTENT_DISPOSITION,
                      "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded));
    }

    @Test
    @WithMockUser
    @DisplayName("Arquivo com conteúdo binário é retornado corretamente")
    void arquivoBinario() throws Exception {
      byte[] binaryContent = new byte[]{0, 1, 2, 3, 4};

      given(serverFilesService.downloadFile(anyLong(), anyString()))
              .willReturn(
                      new MediaDto(
                              MediaType.APPLICATION_OCTET_STREAM,
                              new ByteArrayResource(binaryContent),
                              Path.of("/binario.bin")
                      )
              );

      mockMvc.perform(get("/api/servers/{serverId}/files/download?path={path}", TEST_SERVER_ID, "/binario.bin")
                      .accept(MediaType.APPLICATION_OCTET_STREAM))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
              .andExpect(content().bytes(binaryContent));
    }

    @Test
    @WithMockUser
    @DisplayName("Erro de serviço retorna 500")
    void erroDeServico() throws Exception {
      given(serverFilesService.downloadFile(anyLong(), anyString()))
              .willThrow(new IOException("Erro ao baixar arquivo"));

      mockMvc.perform(get("/api/servers/{serverId}/files/download?path={path}", TEST_SERVER_ID, "/file.txt"))
              .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(get("/api/servers/{serverId}/files/download?path={path}", TEST_SERVER_ID, "/file.txt"))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(get("/api/servers/{serverId}/files/download?path={path}", TEST_SERVER_ID, "/file.txt"))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Download em ZIP")
  class DownloadZipTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - retorna ZIP com headers corretos")
    void sucesso() throws Exception {
      String path = "/arquivos/projeto";

      org.mockito.Mockito.doNothing()
              .when(serverFilesService).downloadFolder(anyLong(), anyString(), any());

      mockMvc.perform(get("/api/servers/{serverId}/files/download-zip?path={path}", TEST_SERVER_ID, path))
              .andExpect(status().isOk())
              .andExpect(content().contentType("application/zip"))
              .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"files.zip\""));
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(get("/api/servers/{serverId}/files/download-zip?path={path}", TEST_SERVER_ID, "/projeto"))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(get("/api/servers/{serverId}/files/download-zip?path={path}", TEST_SERVER_ID, "/projeto"))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Upload de Arquivo")
  class UploadFileTests {

    private MockMultipartFile file;

    @BeforeEach
    void setUpFile() {
      file = new MockMultipartFile(
              "file",
              "upload.txt",
              MediaType.APPLICATION_OCTET_STREAM_VALUE,
              "Conteúdo enviado".getBytes(StandardCharsets.UTF_8)
      );
    }

    @Test
    @WithMockUser
    @DisplayName("Sucesso - envia arquivo único")
    void sucesso() throws Exception {
      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload?path={path}", TEST_SERVER_ID, "/uploads/")
                      .file(file))
              .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Conteúdo vazio ainda é aceito")
    void conteudoVazio() throws Exception {
      MockMultipartFile emptyFile = new MockMultipartFile(
              "file",
              "empty.txt",
              MediaType.TEXT_PLAIN_VALUE,
              new byte[0]
      );

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload?path={path}", TEST_SERVER_ID, "/")
                      .file(emptyFile))
              .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Falha no upload retorna erro de servidor")
    void falhaNoUpload() throws Exception {
      doThrow(new IOException("Erro ao enviar arquivo"))
              .when(serverFilesService).uploadFile(anyLong(), anyString(), any());

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload?path={path}", TEST_SERVER_ID, "/uploads/")
                      .file(file))
              .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload?path={path}", TEST_SERVER_ID, "/uploads/")
                      .file(file))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload?path={path}", TEST_SERVER_ID, "/uploads/")
                      .file(file))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Upload em ZIP")
  class UploadZipTests {

    private MockMultipartFile file;

    @BeforeEach
    void setUpFile() {
      file = new MockMultipartFile(
              "file",
              "backup.zip",
              MediaType.APPLICATION_OCTET_STREAM_VALUE,
              "Example content".getBytes(StandardCharsets.UTF_8)
      );
    }

    @Test
    @WithMockUser
    @DisplayName("Sucesso - envia arquivo ZIP")
    void sucesso() throws Exception {
      String path = "/arquivos/backup";

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload-zip?path={path}", TEST_SERVER_ID, path)
                      .file(file))
              .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Falha no upload retorna erro de servidor")
    void falhaNoUpload() throws Exception {
      doThrow(new IOException("Erro ao enviar pasta"))
              .when(serverFilesService).uploadFolder(anyLong(), anyString(), any());

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload-zip?path={path}", TEST_SERVER_ID, "/backup")
                      .file(file))
              .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload-zip?path={path}", TEST_SERVER_ID, "/backup")
                      .file(file))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(multipart("/api/servers/{serverId}/files/upload-zip?path={path}", TEST_SERVER_ID, "/backup")
                      .file(file))
              .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("Deletar Arquivo")
  class DeleteFileTests {

    @Test
    @WithMockUser
    @DisplayName("Sucesso - deleta arquivo e repassa parâmetros ao service")
    void sucesso() throws Exception {
      String path = "/arquivos/arquivo.txt";

      mockMvc.perform(delete("/api/servers/{serverId}/files/delete?path={path}", TEST_SERVER_ID, path))
              .andExpect(status().isNoContent());

      org.mockito.Mockito.verify(serverFilesService).delete(TEST_SERVER_ID, path);
    }

    @Test
    @WithMockUser
    @DisplayName("Caminho com subdiretório é repassado ao service")
    void caminhoComSubdiretorio() throws Exception {
      String path = "/subdir/arquivo.txt";

      mockMvc.perform(delete("/api/servers/{serverId}/files/delete?path={path}", TEST_SERVER_ID, path))
              .andExpect(status().isNoContent());

      org.mockito.Mockito.verify(serverFilesService).delete(TEST_SERVER_ID, path);
    }

    @Test
    @WithMockUser
    @DisplayName("Falha ao deletar retorna erro de servidor")
    void falhaAoDeletar() throws Exception {
      String path = "/arquivos/arquivo.txt";

      doThrow(new IOException("Erro ao deletar"))
              .when(serverFilesService).delete(anyLong(), anyString());

      mockMvc.perform(delete("/api/servers/{serverId}/files/delete?path={path}", TEST_SERVER_ID, path))
              .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao() throws Exception {
      mockMvc.perform(delete("/api/servers/{serverId}/files/delete?path={path}", TEST_SERVER_ID, "/arquivo.txt"))
              .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é dono retorna 403")
    void naoEhDono() throws Exception {
      given(serverSecurityExpressions.isOwner(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(delete("/api/servers/{serverId}/files/delete?path={path}", TEST_SERVER_ID, "/arquivo.txt"))
              .andExpect(status().isForbidden());
    }
  }

}