package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.model.Player;
import br.com.mc_manager.server.repository.PlayerRepository;
import br.com.mc_manager.server.repository.ServerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

  private static final String PLAYER_ICON_BASE_URL = "https://mc-heads.net/avatar/";
  private final PlayerRepository repository;
  private final ServerRepository serverRepository;

  public PlayerService(PlayerRepository repository, ServerRepository serverRepository) {
    this.repository = repository;
    this.serverRepository = serverRepository;
  }

  public List<PlayerDTO> listServerPlayers(Long serverId) {
    var players = repository.findByServerId(serverId);

    return players.stream().map(PlayerDTO::new).toList();
  }

  public Player getServerPlayer(Long serverId, String nickname) {
    var existsPlayer = repository.existsByServerIdAndNickname(serverId, nickname);

    if (!existsPlayer) {
      var player = saveServerPlayer(serverId, nickname);

      return player;
    }

    var player = repository.findByServerIdAndNickname(serverId, nickname).orElseThrow();

    return player;
  }

  private Player saveServerPlayer(Long serverId, String nickname) {
    var server = serverRepository.findById(serverId).orElseThrow();

    var player = new Player(
            nickname,
            PLAYER_ICON_BASE_URL + nickname + "/48",
            server
    );

    return repository.save(player);
  }

  public void playerConnected(Player player) {
    player.playerConnected();

    repository.save(player);
  }

  public void playerDisconnected(Player player) {
    player.playerDisconnected();

    repository.save(player);
  }

  public void opPlayer(Player player) {
    player.operator();

    repository.save(player);
  }

  public void deopPlayer(Player player) {
    player.deOperator();

    repository.save(player);
  }

  public void banPlayer(Player player) {
    player.banned();

    repository.save(player);
  }

  public void unbanPlayer(Player player) {
    player.unBanned();

    repository.save(player);
  }

}
