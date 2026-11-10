package com.daesabu.meongcoach.support.adapter.websocket;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.support.domain.ChatAccess;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
	private final Map<String, Client> clients = new ConcurrentHashMap<>();

	public List<String> getSubProtocols() {
		return List.of("support.v1");
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws IOException {
		Optional<ChatAccess> access = access(session.getPrincipal());
		if (access.isEmpty()) {
			session.close(CloseStatus.POLICY_VIOLATION);
			return;
		}
		session.setTextMessageSizeLimit(128);
		var wrapped = new ConcurrentWebSocketSessionDecorator(session, 5000, 8192);
		clients.put(session.getId(), new Client(wrapped, access.get()));
		send(session.getId(), "sync");
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		if ("ping".equals(message.getPayload())) {
			send(session.getId(), "pong");
			return;
		}
		close(session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		clients.remove(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		close(session.getId());
	}

	private void send(String id, String text) {
		Client client = clients.get(id);
		if (client == null) {
			return;
		}
		if (client.access().expiresAt().isBefore(Instant.now())) {
			close(id);
			return;
		}
		try {
			client.session().sendMessage(new TextMessage(text));
		} catch (IOException | RuntimeException exception) {
			close(id);
		}
	}

	private void close(String id) {
		Client client = clients.remove(id);
		if (client == null) {
			return;
		}
		try {
			client.session().close(CloseStatus.GOING_AWAY);
		} catch (IOException exception) {
		}
	}

	@PreDestroy
	public void shutdown() {
		clients.keySet().forEach(this::close);
	}

	private static Optional<ChatAccess> access(Principal principal) {
		if (!(principal instanceof JwtAuthenticationToken token)
				|| token.getToken().getExpiresAt() == null) {
			return Optional.empty();
		}
		try {
			long userId = Long.parseLong(token.getName());
			boolean operator =
					token.getAuthorities().stream()
							.anyMatch(a -> AuthorityRole.ADMIN.authority().equals(a.getAuthority()));
			return Optional.of(new ChatAccess(userId, operator, token.getToken().getExpiresAt()));
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}

	private record Client(WebSocketSession session, ChatAccess access) {}
}
