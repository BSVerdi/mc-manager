package br.com.mc_manager.server.model;

import br.com.mc_manager.server.dto.server.CreateServerDTO;
import br.com.mc_manager.server.dto.server.UpdateServerDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "servers")
@Getter
@Builder
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
  private int port;
  private String domain;

  @Enumerated(EnumType.STRING)
  private ServerType type;

  @ManyToOne(fetch = FetchType.LAZY)
  private User owner;

  @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ServerPlayer> players = new HashSet<>();

  private Instant createdAt;

  public Server(CreateServerDTO dto, User owner) {
    this.name = dto.name();
    this.version = dto.version();
    this.port = 25565;
    this.type = dto.type();

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

    this.owner = owner;
    this.javaVersion = "JAVA_21";
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

  public void updateDomain(String domain) {
    this.domain = domain;
  }
}
