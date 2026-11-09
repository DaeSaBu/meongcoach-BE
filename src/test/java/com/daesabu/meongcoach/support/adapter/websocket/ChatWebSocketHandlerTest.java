package com.daesabu.meongcoach.support.adapter.websocket;

import static org.mockito.Mockito.*;

import com.daesabu.meongcoach.support.application.provided.ChatFinder;
import com.daesabu.meongcoach.support.domain.ChatAccess;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class ChatWebSocketHandlerTest {
	private final ChatFinder finder = mock(ChatFinder.class);
	private final ChatWebSocketHandler handler = new ChatWebSocketHandler(finder);

	@AfterEach
	void 종료한다() {
		handler.shutdown();
	}

	private WebSocketSession session(String id, long user) {
		var session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		when(session.isOpen()).thenReturn(true);
		when(session.getAttributes())
				.thenReturn(
						Map.of(
								"access",
								new ChatAccess(user, false, Instant.now().plusSeconds(30))));
		when(finder.isSocketAuthorized(any())).thenReturn(true);
		return session;
	}

	@Test
	void 네번째_동시_연결은_거절한다() throws Exception {
		handler.afterConnectionEstablished(session("1", 42));
		handler.afterConnectionEstablished(session("2", 42));
		handler.afterConnectionEstablished(session("3", 42));
		var fourth = session("4", 42);
		handler.afterConnectionEstablished(fourth);
		verify(fourth).close(CloseStatus.POLICY_VIOLATION);
	}

	@Test
	void 권한이_사라지면_하트비트에서_연결을_닫는다() throws Exception {
		var session = session("1", 42);
		handler.afterConnectionEstablished(session);
		verify(session, timeout(1000)).sendMessage(any());
		when(finder.isSocketAuthorized(any())).thenReturn(false);
		handler.handleMessage(session, new TextMessage("ping"));
		verify(session, timeout(1000)).close(CloseStatus.GOING_AWAY);
	}

	@Test
	void 전송에_실패한_연결은_닫는다() throws Exception {
		var session = session("1", 42);
		doThrow(new java.io.IOException("전송 실패")).when(session).sendMessage(any());
		handler.afterConnectionEstablished(session);
		verify(session, timeout(1000)).close(CloseStatus.GOING_AWAY);
	}

	@Test
	void 임의_소켓_메시지는_거절한다() throws Exception {
		var session = session("1", 42);
		handler.afterConnectionEstablished(session);
		handler.handleMessage(session, new TextMessage("본문"));
		verify(session, timeout(1000)).close(CloseStatus.GOING_AWAY);
	}
}
