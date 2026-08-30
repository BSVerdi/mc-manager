package br.com.mc_manager.server.service;

import br.com.mc_manager.server.dto.PlayerDTO;
import br.com.mc_manager.server.model.Player;
import br.com.mc_manager.server.model.Server;
import br.com.mc_manager.server.model.ServerPlayer;
import br.com.mc_manager.server.model.ServerPlayerId;
import br.com.mc_manager.server.repository.PlayerRepository;
import br.com.mc_manager.server.repository.ServerPlayerRepository;
import br.com.mc_manager.server.repository.ServerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class PlayerService {

  private static final String PLAYER_ICON_BASE_URL = "https://mc-heads.net/avatar/";
  private final PlayerRepository playerRepository;
  private final ServerRepository serverRepository;
  private final ServerPlayerRepository serverPlayerRepository;

  public PlayerService(PlayerRepository repository, ServerRepository serverRepository, ServerPlayerRepository serverPlayerRepository) {
    this.playerRepository = repository;
    this.serverRepository = serverRepository;
    this.serverPlayerRepository = serverPlayerRepository;
  }

  public List<PlayerDTO> listServerPlayers(Long serverId) {
    List<ServerPlayer> players = serverPlayerRepository.findByServerId(serverId);

    return players.stream().map(PlayerDTO::new).toList();
  }

  public void saveServerPlayer(Long serverId, String nickname) {
    Player player = playerRepository.findByNickname(nickname)
            .orElseGet(() -> playerRepository.save(
                    new Player(nickname, PLAYER_ICON_BASE_URL + nickname + "/48")
            ));

    ServerPlayerId serverPlayerId = new ServerPlayerId(serverId, player.getId());
    boolean alreadyLinked = serverPlayerRepository.existsById(serverPlayerId);

    if (!alreadyLinked) {
      Server server = serverRepository.findById(serverId).orElseThrow();
      ServerPlayer memberShip = new ServerPlayer(server, player);

      serverPlayerRepository.save(memberShip);
    }
  }

  public void playerConnected(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::playerConnected);
  }

  public void playerDisconnected(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::playerDisconnected);
  }

  public void opPlayer(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::operator);
  }

  public void deopPlayer(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::deOperator);
  }

  public void banPlayer(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::banned);
  }

  public void unbanPlayer(Long serverId, String nickname) {
    updateStatus(serverId, nickname, ServerPlayer::unBanned);
  }

  @Transactional
  private void updateStatus(Long serverId, String nickname, Consumer<ServerPlayer> action) {
    ServerPlayer serverPlayer = findServerPlayer(serverId, nickname);

    action.accept(serverPlayer);
  }

  private ServerPlayer findServerPlayer(Long serverId, String nickname) {
    Player player = playerRepository.findByNickname(nickname)
            .orElseThrow(() -> new IllegalArgumentException("Player não encontrado!"));

    ServerPlayerId serverPlayerId = new ServerPlayerId(serverId, player.getId());
    ServerPlayer serverPlayer = serverPlayerRepository.findById(serverPlayerId)
            .orElseThrow(() -> new IllegalArgumentException("Player não está associado ao servidor!"));

    return serverPlayer;
  }

}
