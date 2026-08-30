package br.com.mc_manager.server.model;

import br.com.mc_manager.server.dto.UpdateUserDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String email;
  private String password;
  private String profilePictureUrl;

  private Boolean enabled;
  private Instant createdAt;
  private Instant updatedAt;


  @Enumerated(EnumType.STRING)
  private Profile profile;

  @OneToMany(mappedBy = "owner")
  private List<Server> servers;

  public User(String name, String email, String encryptedPassword, Profile profile) {
    this.name = name;
    this.email = email;
    this.password = encryptedPassword;
    this.profile = profile;
    this.enabled = true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + profile.name()));
  }

  @Override
  public @Nullable String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return this.email;
  }

  public void updateUser(UpdateUserDto dto) {
    this.name = dto.username();

    this.updatedAt = Instant.now();
  }

  public void deactivate() {
    this.enabled = false;
  }

  public void updateProfilePicUrl(String path) {
    this.profilePictureUrl = path;
    this.updatedAt = Instant.now();
  }
}
