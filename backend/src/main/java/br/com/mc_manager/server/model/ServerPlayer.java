package br.com.mc_manager.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "server_players")
@Getter
@NoArgsConstructor
public class ServerPlayer {

  @EmbeddedId
  private ServerPlayerId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("serverId")
  @JoinColumn(name = "server_id")
  private Server server;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("playerId")
  @JoinColumn(name = "player_id")
  private Player player;

  private boolean banned = false;
  private boolean operator = false;
  private boolean online = true;
  private Instant lastLogin;

  public ServerPlayer(Server server, Player player) {
    this.id = new ServerPlayerId(server.getId(), player.getId());
    this.server = server;
    this.player = player;
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
