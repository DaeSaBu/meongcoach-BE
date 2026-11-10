package com.daesabu.meongcoach.support.adapter.websocket;

import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class ChatWebSocketHandlerTest {
	private final ChatWebSocketHandler handler = new ChatWebSocketHandler();

	@AfterEach
	void 종료한다() {
		handler.shutdown();
	}

	private Principal principal(long user, Instant expiresAt) {
		Jwt jwt =
				Jwt.withTokenValue("access-token")
						.header("alg", "HS256")
						.subject(Long.toString(user))
						.issuedAt(expiresAt.minusSeconds(3600))
						.expiresAt(expiresAt)
						.build();
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	private WebSocketSession session(String id, Principal principal) {
		var session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		when(session.isOpen()).thenReturn(true);
		when(session.getPrincipal()).thenReturn(principal);
		return session;
	}

	private WebSocketSession session(String id) {
		return session(id, principal(42, Instant.now().plusSeconds(3600)));
	}

	@Test
	void 연결하면_sync를_보낸다() throws Exception {
		var session = session("1");
		handler.afterConnectionEstablished(session);
		verify(session).sendMessage(new TextMessage("sync"));
	}

	@Test
	void ping에는_pong으로_응답한다() throws Exception {
		var session = session("1");
		handler.afterConnectionEstablished(session);
		handler.handleMessage(session, new TextMessage("ping"));
		verify(session).sendMessage(new TextMessage("pong"));
	}

	@Test
	void 인증_정보가_없으면_연결을_거절한다() throws Exception {
		var session = session("1", null);
		handler.afterConnectionEstablished(session);
		verify(session).close(CloseStatus.POLICY_VIOLATION);
		verify(session, never()).sendMessage(any());
	}

	@Test
	void 토큰이_만료되면_연결을_닫는다() throws Exception {
		var session = session("1", principal(42, Instant.now().plusMillis(200)));
		handler.afterConnectionEstablished(session);
		Thread.sleep(300);
		handler.handleMessage(session, new TextMessage("ping"));
		verify(session).close(CloseStatus.GOING_AWAY);
		verify(session, never()).sendMessage(new TextMessage("pong"));
	}

	@Test
	void 전송에_실패한_연결은_닫는다() throws Exception {
		var session = session("1");
		doThrow(new java.io.IOException("전송 실패")).when(session).sendMessage(any());
		handler.afterConnectionEstablished(session);
		verify(session).close(CloseStatus.GOING_AWAY);
	}

	@Test
	void 임의_소켓_메시지는_거절한다() throws Exception {
		var session = session("1");
		handler.afterConnectionEstablished(session);
		handler.handleMessage(session, new TextMessage("본문"));
		verify(session).close(CloseStatus.GOING_AWAY);
	}
}
