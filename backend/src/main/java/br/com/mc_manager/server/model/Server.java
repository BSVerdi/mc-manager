package br.com.mc_manager.server.model;

import br.com.mc_manager.server.dto.CreateServerDTO;
import br.com.mc_manager.server.dto.UpdateServerDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "servers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Server {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String version;
  private String javaVersion;
  private String minRam;
  private String maxRam;
  private Integer port;

  @ManyToOne(fetch = FetchType.LAZY)
  private User owner;

  @OneToMany(mappedBy = "server", cascade = CascadeType.REMOVE)
  private List<Player> players;

  private Instant createdAt;

  public Server(CreateServerDTO dto, User owner) {
    this.name = dto.name();
    this.version = dto.version();
    this.port = dto.port();

    if (dto.minRam() != null) {
      this.minRam = dto.minRam();
    } else {
      this.minRam = "1G";
    }

    if (dto.maxRam() != null) {
      this.maxRam = dto.maxRam();
    } else {
      this.maxRam = "2G";
    }

    javaVersion = "JAVA_21";
    this.owner = owner;
  }

  @PrePersist
  void onCreate() {
    this.createdAt = Instant.now();
  }

  public void update(UpdateServerDTO dto) {
    this.name = dto.name();

    if (dto.minRam() != null) {
      this.minRam = dto.minRam();
    }

    if (dto.maxRam() != null) {
      this.maxRam = dto.maxRam();
    }
  }

  public void updateJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

}
