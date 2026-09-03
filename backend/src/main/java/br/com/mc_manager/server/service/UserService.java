package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.UpdateUserDto;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.model.User;
import br.com.mc_manager.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

  private final UserRepository repository;
  private final FileStorageService fileService;

  public UserService(UserRepository repository, FileStorageService fileService) {
    this.repository = repository;
    this.fileService = fileService;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return repository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
  }

  public User currentUser(Principal principal) {
    return repository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
  }

  @Transactional
  public UserDetailsDTO updateUser(Long id, UpdateUserDto dto) {
    User user = getUserById(id);

    user.updateUser(dto);

    return new UserDetailsDTO(user);
  }

  @Transactional
  public void deleteUser(Long id) {
    User user = getUserById(id);
    user.deactivate();
  }

  @Transactional
  public UserDetailsDTO updateProfilePic(Long id, MultipartFile file) throws IOException {
    User user = getUserById(id);

    if (!file.isEmpty()) {
      deleteOldPicture(user);

      String path = saveProfilePic(file);
      user.updateProfilePicUrl(path);
    }

    return new UserDetailsDTO(user);
  }

  private User getUserById(Long id) {
    return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
  }

  private void deleteOldPicture(User user) throws IOException {
    if (user.getProfilePictureUrl() == null) return;

    String oldPicFileName = user.getProfilePictureUrl().replaceAll("images/", "");
    fileService.deleteFile("images/", oldPicFileName);
  }

  private String saveProfilePic(MultipartFile file) throws IOException {
    String extension = fileService.extractExtension(file.getOriginalFilename());
    String fileName = UUID.randomUUID() + "." + extension;

    fileService.saveFile("images/", fileName, file);

    return "images/" + fileName;
  }

}
