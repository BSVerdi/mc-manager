package br.com.mc_manager.server.service.runtime;

import br.com.mc_manager.server.service.PlayerService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerConsoleLineProcessor implements Consumer<String> {
  private record ParsedLine(LogLevel level, String payload) {
  }

  // remove código ANSI e \r
  private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

  // [08:23:24  |  2026-09-18 04:54:30  (hora e data opcionalmente com colchete)
  private static final Pattern TIME_PATTERN = Pattern.compile(
          "^(?:\\[?\\d{2}:\\d{2}:\\d{2}|\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");

  /// Paper: " INFO]: "  |  Vanilla: "] [Server thread/INFO]: "  |  formato com data: " [INFO] "
  private static final Pattern LEVEL_PATTERN = Pattern.compile(
          "^(?:\\s+(?<paper>[A-Z]+)\\]|\\]?\\s*\\[(?:[^\\]/]*/)?(?<other>[A-Z]+)\\])\\s*:?\\s*");

  // [INFO] Breninjos[/111.111.111.111] logged in with entity id 855 at (0 67.0, 0)
  private static final Pattern JOIN_PATTERN =
          Pattern.compile("^(\\w+)\\[/[^\\]]+] logged in with entity id \\d+ at \\(.*\\)");

  // [INFO] Breninjos lost connection: disconnect.quitting
  private static final Pattern LEAVE_PATTERN =
          Pattern.compile("^(\\w+) lost connection: .*$");

  private final Long serverId;
  private final ServerBroadcaster broadcaster;
  private final PlayerService playerService;
  private final Set<String> onlinePlayers = ConcurrentHashMap.newKeySet();

  public ServerConsoleLineProcessor(Long serverId, ServerBroadcaster broadcaster, PlayerService playerService) {
    this.serverId = serverId;
    this.broadcaster = broadcaster;
    this.playerService = playerService;
  }

  private ParsedLine parse(String line) {
    Matcher time = TIME_PATTERN.matcher(line);
    if (!time.find()) return new ParsedLine(LogLevel.UNKNOWN, line);

    String rest = line.substring(time.end());

    Matcher level = LEVEL_PATTERN.matcher(rest);
    if (!level.find()) return new ParsedLine(LogLevel.UNKNOWN, rest.strip());

    String lvl = level.group("paper") != null ? level.group("paper") : level.group("other");
    return new ParsedLine(LogLevel.from(lvl), rest.substring(level.end()));
  }

  @Override
  public void accept(String rawLine) {
    ParsedLine line = parse(rawLine);
    trackPlayerEvents(line.payload());
    broadcaster.consoleLine(line.level(), line.payload());
  }

  public Integer getOnlinePlayersCount() {
    return onlinePlayers.size();
  }

  public void disconnectAllPlayers() {
    onlinePlayers.forEach(nickname -> playerService.playerDisconnected(serverId, nickname));
    onlinePlayers.clear();
  }


  private void trackPlayerEvents(String clean) {
    Matcher joinMatcher = JOIN_PATTERN.matcher(clean);

    if (joinMatcher.matches()) {
      String playerNickname = joinMatcher.group(1);

      playerService.saveServerPlayer(serverId, playerNickname);
      playerService.playerConnected(serverId, playerNickname);
      onlinePlayers.add(playerNickname);

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
