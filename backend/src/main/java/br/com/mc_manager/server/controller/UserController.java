package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.UpdateUserDto;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PatchMapping("/{id}/update-user")
  public ResponseEntity<UserDetailsDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDto dto) {
    var user = userService.updateUser(id, dto);

    return ResponseEntity.ok().body(user);
  }

  @PatchMapping("/{id}/profile-picture")
  public ResponseEntity<UserDetailsDTO> updateProfilePic(@PathVariable Long id, @RequestParam(value = "file") MultipartFile file) throws IOException {
    var user = userService.updateProfilePic(id, file);

    return ResponseEntity.ok().body(user);
  }

  @DeleteMapping("/{id}/delete")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);

    return ResponseEntity.noContent().build();
  }

}
