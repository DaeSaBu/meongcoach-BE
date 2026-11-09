package com.daesabu.meongcoach.support.adapter.websocket;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.domain.ChatAccess;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {
	private final ChatFinder finder;
	private final Map<String, Client> clients = new ConcurrentHashMap<>();
	private final ExecutorService delivery =
			new ThreadPoolExecutor(
					4,
					4,
					0,
					TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(512),
					Thread.ofPlatform().daemon().name("support-delivery-", 0).factory());

	public List<String> getSubProtocols() {
		return List.of("support.v1");
	}

	@Override
	public synchronized void afterConnectionEstablished(WebSocketSession session)
			throws IOException {
		ChatAccess access = (ChatAccess) session.getAttributes().get("access");
		if (access == null || clients.size() >= 256) {
			session.close(CloseStatus.POLICY_VIOLATION);
			return;
		}
		long count =
				clients.values().stream()
						.filter(c -> c.access().userId() == access.userId())
						.count();
		if (count >= 3) {
			session.close(CloseStatus.POLICY_VIOLATION);
			return;
		}
		if (session instanceof StandardWebSocketSession standard
				&& standard.getNativeSession() != null) {
			var properties = standard.getNativeSession().getUserProperties();
			properties.put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 5000L);
			properties.put("org.apache.tomcat.websocket.SESSION_CLOSE_TIMEOUT", 1000L);
		}
		session.setTextMessageSizeLimit(128);
		var wrapped = new ConcurrentWebSocketSessionDecorator(session, 5000, 8192);
		clients.put(session.getId(), new Client(wrapped, access, Instant.now().plusSeconds(900)));
		send(session.getId(), "sync");
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		Client client = clients.get(session.getId());
		if (client == null) {
			return;
		}
		if (client.expiresAt().isBefore(Instant.now())) {
			close(session.getId());
			return;
		}
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

	public void changed(long conversationId) {
		clients.forEach(
				(id, client) -> {
					if (client.access().operator() || client.access().userId() == conversationId) {
						send(id, "changed");
					}
				});
	}

	public void synchronizeAll() {
		clients.forEach(
				(id, client) -> {
					if (client.expiresAt().isBefore(Instant.now())) {
						close(id);
						return;
					}
					send(id, "sync");
				});
	}

	private void send(String id, String text) {
		try {
			delivery.execute(
					() -> {
						Client client = clients.get(id);
						if (client == null) {
							return;
						}
						try {
							if (client.expiresAt().isBefore(Instant.now())
									|| (("sync".equals(text) || "pong".equals(text))
											&& !finder.isSocketAuthorized(client.access()))) {
								close(id);
								return;
							}
							client.session().sendMessage(new TextMessage(text));
						} catch (IOException | RuntimeException exception) {
							close(id);
						}
					});
		} catch (RejectedExecutionException exception) {
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
		delivery.shutdownNow();
	}

	private record Client(WebSocketSession session, ChatAccess access, Instant expiresAt) {}
}
