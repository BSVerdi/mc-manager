package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.UpdateUserDto;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.factory.UserFactory;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private FileStorageService fileStorageService;

  @Mock
  private Principal principal;

  @Mock
  private MultipartFile multipartFile;

  @InjectMocks
  private UserService userService;

  private User validUser;
  private Optional<User> optionalValidUser;

  @BeforeEach
  void setUp() {
    // Arrange - Criar usuário válido
    this.validUser = UserFactory.createUserWithAllAttributes();

    this.optionalValidUser = Optional.of(validUser);
  }

  @Nested
  class LoadUserByUsername {

    @Test
    @DisplayName("Deve carregar UserDetails ao buscar usuário por email válido")
    void testLoadUserByUsernameSuccess() {
      // Arrange
      String email = validUser.getEmail();
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(optionalValidUser);

      // Act
      UserDetails userDetails = userService.loadUserByUsername(email);

      // Assert
      assertThat(userDetails).isNotNull();
      assertThat(userDetails.getUsername()).isEqualTo(email);
      verify(userRepository, times(1)).findByEmailIgnoreCase(email);
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException ao buscar usuário inexistente")
    void testLoadUserByUsernameNotFound() {
      // Arrange
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
      String email = "nonexistent@example.com";

      // Act & Assert
      assertThatThrownBy(() -> userService.loadUserByUsername(email))
              .isInstanceOf(UsernameNotFoundException.class)
              .hasMessage("User not found with username: " + email);

      verify(userRepository, times(1)).findByEmailIgnoreCase(email);
    }
  }

  @Nested
  class CurrentUser {

    @Test
    @DisplayName("Deve retornar User quando principal tem nome de email válido")
    void testCurrentUserSuccess() {
      // Arrange
      when(principal.getName()).thenReturn("joao@example.com");
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(optionalValidUser);

      // Act
      User user = userService.currentUser(principal);

      // Assert
      assertThat(user).isEqualTo(validUser);
      verify(userRepository, times(1)).findByEmailIgnoreCase("joao@example.com");
    }

    @Test
    @DisplayName("Deve lançar exceção quando principal tem email inexistente")
    void testCurrentUserNotFound() {
      // Arrange
      when(principal.getName()).thenReturn("nonexistent@example.com");
      when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.currentUser(principal))
              .isInstanceOf(NoSuchElementException.class)
              .hasMessage("No value present");

      verify(userRepository, times(1)).findByEmailIgnoreCase("nonexistent@example.com");
    }
  }

  @Nested
  class UpdateUser {

    @Test
    @DisplayName("Deve atualizar usuário e retornar UserDetailsDTO com novo nome")
    void testUpdateUserSuccess() {
      // Arrange - Criar DTO de atualização
      UpdateUserDto updateDto = new UpdateUserDto("João Updated");
      when(userRepository.findById(anyLong())).thenReturn(optionalValidUser);

      // Act
      UserDetailsDTO result = userService.updateUser(1L, updateDto);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.username()).isEqualTo("João Updated");
      assertThat(validUser.getName()).isEqualTo(updateDto.username());
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException ao tentar atualizar usuário inexistente")
    void testUpdateUserNotFound() {
      // Arrange - Usuário não existe (findById retorna Optional.empty)
      when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

      UpdateUserDto updateDto = new UpdateUserDto("New Name");

      // Act & Assert
      assertThatThrownBy(() -> userService.updateUser(999L, updateDto))
              .isInstanceOf(NoSuchElementException.class)
              .hasMessage("Usuário não encontrado.");

      verify(userRepository, times(1)).findById(999L);
    }
  }

  @Nested
  class DeleteUser {

    @Test
    @DisplayName("Deve desativar usuário existente ao deletar")
    void testDeleteUserSuccess() {
      // Arrange - Usuário ativo inicialmente
      when(userRepository.findById(anyLong())).thenReturn(optionalValidUser);
      assertThat(validUser.getEnabled()).isTrue();

      // Act
      userService.deleteUser(1L);

      // Assert
      assertThat(validUser.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException ao tentar deletar usuário inexistente")
    void testDeleteUserNotFound() {
      // Arrange - Usuário não existe
      when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.deleteUser(999L))
              .isInstanceOf(NoSuchElementException.class)
              .hasMessage("Usuário não encontrado.");

      verify(userRepository, times(1)).findById(999L);
    }
  }

  @Nested
  class UpdateProfilePic {

    @Test
    @DisplayName("Deve atualizar URL de perfil quando arquivo não estiver vazio")
    void testUpdateProfilePicWithFile() throws IOException {
      // Arrange - Criar mock de arquivo com tamanho não-zero
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getOriginalFilename()).thenReturn("new-pic.jpg");
      when(userRepository.findById(anyLong())).thenReturn(optionalValidUser);

      // Act
      UserDetailsDTO result = userService.updateProfilePic(1L, multipartFile);

      // Assert
      assertThat(result).isNotNull();
      verify(fileStorageService).saveFile(anyString(), anyString(), any(MultipartFile.class));
      verify(fileStorageService).deleteFile(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve retornar UserDetailsDTO sem atualizar quando arquivo estiver vazio")
    void testUpdateProfilePicWithEmptyFile() throws IOException {
      // Arrange - Criar mock de arquivo vazio
      when(multipartFile.isEmpty()).thenReturn(true);
      when(userRepository.findById(anyLong())).thenReturn(optionalValidUser);

      // Act
      UserDetailsDTO result = userService.updateProfilePic(1L, multipartFile);

      // Assert
      assertThat(result).isNotNull();
      verify(fileStorageService, never()).deleteFile(anyString(), anyString());
      verify(fileStorageService, never()).saveFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException ao atualizar perfil de usuário inexistente")
    void testUpdateProfilePicUserNotFound() throws IOException {
      // Arrange - Usuário não existe
      when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.updateProfilePic(999L, multipartFile))
              .isInstanceOf(NoSuchElementException.class)
              .hasMessage("Usuário não encontrado.");
    }
  }

}