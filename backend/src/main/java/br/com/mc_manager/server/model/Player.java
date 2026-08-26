package br.com.mc_manager.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nickname;
  private String icon;
  private Boolean banned;
  private Boolean operator;
  private Boolean online;
  private Instant lastLogin;

  @ManyToOne(fetch = FetchType.LAZY)
  private Server server;

  public Player(String nickname, String icon, Server server) {
    this.nickname = nickname;
    this.icon = icon;
    this.server = server;

    this.online = true;
    this.banned = false;
    this.operator = false;

    this.lastLogin = null;
  }

  public void playerDisconnected() {
    this.online = false;
  }

  public void playerConnected() {
    this.online = true;
  }

  public void banned() {
    this.banned = true;
  }

  public void unBanned() {
    this.banned = false;
  }

  public void operator() {
    this.operator = true;
  }

  public void deOperator() {
    this.operator = false;
  }

}
