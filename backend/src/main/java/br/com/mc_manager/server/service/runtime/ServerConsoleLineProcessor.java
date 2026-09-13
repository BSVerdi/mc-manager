package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.service.PlayerService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerConsoleLineProcessor implements Consumer<String> {

  private static final Pattern LEVEL_PREFIX_PATTERN =
          Pattern.compile("^(?:\\[\\d{2}:\\d{2}:\\d{2}]|.*?) (\\[.*].*)$");
  private static final Pattern JOIN_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+)\\[/[^\\]]+] logged in with entity id \\d+ at \\(.*\\)");
  private static final Pattern LEAVE_PATTERN =
          Pattern.compile("^(?:\\[.*]).* (\\w+) lost connection: .*$");

  private final Long serverId;
  private final ServerStatusBroadcaster broadcaster;
  private final PlayerService playerService;
  private final Set<String> onlinePlayers = ConcurrentHashMap.newKeySet();

  public ServerConsoleLineProcessor(Long serverId, ServerStatusBroadcaster broadcaster, PlayerService playerService) {
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
    onlinePlayers.forEach(name -> playerService.playerDisconnected(serverId, name));
    onlinePlayers.clear();
  }

  private String stripDatePrefix(String rawLine) {
    Matcher matcher = LEVEL_PREFIX_PATTERN.matcher(rawLine);

    return matcher.matches() ? matcher.group(1) : rawLine;
  }

  private void trackPlayerEvents(String clean) {
    Matcher joinMatcher = JOIN_PATTERN.matcher(clean);

    if (joinMatcher.matches()) {
      String playerName = joinMatcher.group(1);

      playerService.saveServerPlayer(serverId, playerName);
      playerService.playerConnected(serverId, playerName);
      onlinePlayers.add(playerName);

      return;
    }

    Matcher leaveMatcher = LEAVE_PATTERN.matcher(clean);
    if (leaveMatcher.matches()) {
      String playerNickname = leaveMatcher.group(1);
      playerService.playerDisconnected(serverId, playerNickname);
      onlinePlayers.remove(playerNickname);
    }
  }
}
