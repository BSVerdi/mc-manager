package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.UpdateUserDto;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.security.expression.UserSecurityExpression;
import br.com.mc_manager.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static br.com.mc_manager.server.factory.UserDtoFactory.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserSecurityExpression userSecurityExpression;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(get("/").with(csrf()))
            .build();

    given(userSecurityExpression.isUser(anyLong(), any(Authentication.class))).willReturn(true);
  }

  @Nested
  @DisplayName("Atualizar Usuário")
  class UpdateUserTest {

    @Test
    @WithMockUser
    @DisplayName("Deve atualizar usuário com sucesso e retornar 200 OK")
    void deveAtualizarUsuarioComSucesso() throws Exception {
      // Arrange
      Long userId = 1L;
      UpdateUserDto dto = createUpdateUserDtoWithAllAttributes();
      UserDetailsDTO expectedUserDetails = createUserDetailsWithAttributes("New Username", "example@email.com");

      String dtoJson = objectMapper.writeValueAsString(dto);

      given(userService.updateUser(eq(userId), any(UpdateUserDto.class))).willReturn(expectedUserDetails);

      // Act & Assert
      mockMvc.perform(patch("/api/users/1/update-user")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.username").value("New Username"))
              .andExpect(jsonPath("$.email").value("example@email.com"));

      verify(userService).updateUser(eq(userId), any(UpdateUserDto.class));
      verifyNoMoreInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 Not Found quando usuário não existe")
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
      // Arrange
      Long userId = 1L;
      UpdateUserDto dto = createUpdateUserDtoWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(dto);

      doThrow(NoSuchElementException.class).when(userService).updateUser(eq(userId), any(UpdateUserDto.class));

      // Act & Assert
      mockMvc.perform(patch("/api/users/1/update-user")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 400 Bad Request para payload inválido (falta campo obrigatório)")
    void deveRetornarBadRequestParaPayloadInvalido() throws Exception {
      // Arrange — DTO parcialmente preenchido
      UpdateUserDto dto = createUpdateUserDtoWithNullAttributes();
      String invalidJson = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc.perform(patch("/api/users/1/update-user")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(invalidJson))
              .andExpect(status().isBadRequest());

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      UpdateUserDto dto = createUpdateUserDtoWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(dto);

      mockMvc.perform(patch("/api/users/1/update-user")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é o próprio usuário retorna 403")
    void naoEhOProprioUsuario_retorna403() throws Exception {
      given(userSecurityExpression.isUser(anyLong(), any(Authentication.class))).willReturn(false);

      UpdateUserDto dto = createUpdateUserDtoWithAllAttributes();
      String dtoJson = objectMapper.writeValueAsString(dto);

      mockMvc.perform(patch("/api/users/1/update-user")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(dtoJson))
              .andExpect(status().isForbidden());

      verifyNoInteractions(userService);
    }
  }

  @Nested
  @DisplayName("Atualizar Foto de Perfil")
  class UpdateProfilePicTest {

    private final MockMultipartFile validImage = new MockMultipartFile(
            "file",
            "perfil.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "binary-image-data".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    @WithMockUser
    @DisplayName("Deve atualizar foto de perfil com sucesso e retornar 200 OK")
    void deveAtualizarFotoDePerfilComSucesso() throws Exception {
      // Arrange
      Long userId = 1L;
      UserDetailsDTO expectedUserDetails = createUserDetailsWithAttributes("New Username", "example@email.com");

      given(userService.updateProfilePic(eq(userId), any())).willReturn(expectedUserDetails);

      // Act & Assert
      mockMvc.perform(multipart("/api/users/1/profile-picture")
                      .file(validImage))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.username").value("New Username"))
              .andExpect(jsonPath("$.email").value("example@email.com"));

      verify(userService).updateProfilePic(eq(userId), any());
      verifyNoMoreInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 Not Found quando usuário não existe")
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
      // Arrange
      Long userId = 1L;

      doThrow(NoSuchElementException.class).when(userService).updateProfilePic(eq(userId), any());

      // Act & Assert
      mockMvc.perform(multipart("/api/users/1/profile-picture")
                      .file(validImage))
              .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      mockMvc.perform(multipart("/api/users/1/profile-picture")
                      .file(validImage))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é o próprio usuário retorna 403")
    void naoEhOProprioUsuario_retorna403() throws Exception {
      given(userSecurityExpression.isUser(anyLong(), any(Authentication.class))).willReturn(false);

      mockMvc.perform(multipart("/api/users/1/profile-picture")
                      .file(validImage))
              .andExpect(status().isForbidden());

      verifyNoInteractions(userService);
    }
  }

  @Nested
  @DisplayName("Deletar Usuário")
  class DeleteUserTest {

    @Test
    @WithMockUser
    @DisplayName("Deve deletar usuário com sucesso e retornar 204 No Content")
    void deveDeletarUsuarioComSucesso() throws Exception {
      // Arrange
      Long userId = 1L;

      // Act & Assert
      mockMvc.perform(delete("/api/users/1/delete"))
              .andExpect(status().isNoContent());

      verify(userService).deleteUser(userId);
      verifyNoMoreInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Deve retornar 404 Not Found ao tentar deletar usuário inexistente")
    void deveRetornar404AoTentarDeletarUsuarioInexistente() throws Exception {
      // Arrange
      Long userId = 1L;

      doThrow(NoSuchElementException.class).when(userService).deleteUser(userId);

      // Act & Assert
      mockMvc.perform(delete("/api/users/1/delete"))
              .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sem autenticação retorna 401")
    void semAutenticacao_retorna401() throws Exception {
      mockMvc.perform(delete("/api/users/1/delete"))
              .andExpect(status().isUnauthorized());

      verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser
    @DisplayName("Autenticado mas não é o próprio usuário retorna 403")
    void naoEhOProprioUsuario_retorna403() throws Exception {
      // Arrange — sobrescreve o stub padrão do @BeforeEach para simular usuário diferente
      given(userSecurityExpression.isUser(anyLong(), any(Authentication.class))).willReturn(false);

      // Act & Assert
      mockMvc.perform(delete("/api/users/1/delete"))
              .andExpect(status().isForbidden());

      // Autorização negada -> o service nunca deveria ser chamado
      verifyNoInteractions(userService);
    }
  }
}