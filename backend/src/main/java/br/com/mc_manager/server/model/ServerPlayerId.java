package br.com.mc_manager.server.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode()
public class ServerPlayerId implements Serializable {
  private Long serverId;
  private Long playerId;
}
