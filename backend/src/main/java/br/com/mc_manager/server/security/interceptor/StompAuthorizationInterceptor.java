package br.com.mc_manager.server.security.interceptor;

import br.com.mc_manager.server.security.expression.ServerSecurityExpressions;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthorizationInterceptor implements ChannelInterceptor {

  private static final Pattern SERVER_SCOPED_DESTINATION =
          Pattern.compile("^/(?:topic|app)/(?:console|status|metrics|command)/(\\d+)$");
  private static final Set<StompCommand> CHECKED_COMMANDS = Set.of(StompCommand.SUBSCRIBE, StompCommand.SEND);

  private final ServerSecurityExpressions serverSecurity;

  public StompAuthorizationInterceptor(ServerSecurityExpressions serverSecurity) {
    this.serverSecurity = serverSecurity;
  }

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor acessor = StompHeaderAccessor.wrap(message);

    if (!CHECKED_COMMANDS.contains(acessor.getCommand())) {
      return message;
    }

    Long serverId = extractServerId(acessor.getDestination());
    if (serverId == null) {
      return message;
    }


    Principal principal = acessor.getUser();
    if (principal == null) {
      throw new IllegalStateException("Conexão WebSocket sem usuário autenticado");
    }

    var isOwner = serverSecurity.isOwner(serverId, principal.getName());

    if (!isOwner) {
      throw new SecurityException("Voce nao tem permissao para acessar o servidor " + serverId);
    }


    return message;
  }

  private Long extractServerId(@Nullable String destination) {
    if (destination == null) return null;

    Matcher matcher = SERVER_SCOPED_DESTINATION.matcher(destination);

    return matcher.matches() ? Long.parseLong(matcher.group(1)) : null;
  }
}
