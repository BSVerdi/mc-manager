package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.service.PlayerService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerConsoleLineProcessor implements Consumer<String> {

  private static final Pattern LEVEL_PREFIX_PATTERN =
          Pattern.compile("^\\[\\d{2}:\\d{2}:\\d{2}\\] (?<message>\\[[^\\]]*\\](?::\\s*)?.*)$");
  private static final Pattern JOIN_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+)\\[/[^\\]]+] logged in with entity id \\d+ at \\(.*\\)");
  private static final Pattern LEAVE_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+) lost connection: .*$");

  private final Long serverId;
  private final ServerBroadcaster broadcaster;
  private final PlayerService playerService;
  private final Set<String> onlinePlayers = ConcurrentHashMap.newKeySet();

  public ServerConsoleLineProcessor(Long serverId, ServerBroadcaster broadcaster, PlayerService playerService) {
    this.serverId = serverId;
    this.broadcaster = broadcaster;
    this.playerService = playerService;
  }

  @Override
  public void accept(String rawLine) {
    String clean = stripDatePrefix(rawLine);
    trackPlayerEvents(clean);
    broadcaster.consoleLine(clean);
  }

  public Integer getOnlinePlayersCount() {
    return onlinePlayers.size();
  }

  public void disconnectAllPlayers() {
    onlinePlayers.forEach(nickname -> playerService.playerDisconnected(serverId, nickname));
    onlinePlayers.clear();
  }

  private String stripDatePrefix(String rawLine) {
    Matcher matcher = LEVEL_PREFIX_PATTERN.matcher(rawLine);

    return matcher.matches() ? matcher.group("message") : rawLine;
  }

  private void trackPlayerEvents(String clean) {
    Matcher joinMatcher = JOIN_PATTERN.matcher(clean);

    if (joinMatcher.matches()) {
      String playerNickname = joinMatcher.group(1);

      playerService.saveServerPlayer(serverId, playerNickname);
      playerService.playerConnected(serverId, playerNickname);
      onlinePlayers.add(playerNickname);

      broadcaster.players(onlinePlayers);

      return;
    }

    Matcher leaveMatcher = LEAVE_PATTERN.matcher(clean);
    if (leaveMatcher.matches()) {
      String playerNickname = leaveMatcher.group(1);
      playerService.playerDisconnected(serverId, playerNickname);
      onlinePlayers.remove(playerNickname);

      broadcaster.players(onlinePlayers);
    }
  }
}
