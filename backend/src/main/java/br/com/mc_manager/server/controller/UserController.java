package br.com.mc_manager.server.controller;

import br.com.mc_manager.server.dto.RegisterUserDTO;
import br.com.mc_manager.server.dto.UserDetailsDTO;
import br.com.mc_manager.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class UserController {

  @Autowired
  UserService service;

  @PostMapping("/users/register")
  public ResponseEntity<UserDetailsDTO> register(@RequestBody RegisterUserDTO registerUser, UriComponentsBuilder uriBuilder) {
    var user = service.registerUser(registerUser);

    var uri = uriBuilder.path("/api/users/register").buildAndExpand(user.id()).toUri();

    return ResponseEntity.created(uri).body(user);
  }

}
