package com.daesabu.meongcoach.support.adapter.websocket;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.util.ReflectionTestUtils;

@NonTransactionalApplicationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {
	@Autowired UserRepository users;
	@Autowired TokenProvider tokens;
	@LocalServerPort int port;

	private Long member(UserRole role) {
		var user = User.registerUser();
		ReflectionTestUtils.setField(user, "role", role);
		return users.save(user).getId();
	}

	private CompletableFuture<WebSocket> connect(HttpClient http, String token, Inbox inbox) {
		var builder = http.newWebSocketBuilder().subprotocols("support.v1");
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return builder.buildAsync(URI.create("ws://localhost:" + port + "/ws/support"), inbox);
	}

	@Test
	void 실제_100개_연결에서_JWT_인증과_하트비트를_검증한다() throws Exception {
		List<Long> ids = new ArrayList<>();
		List<WebSocket> sockets = new ArrayList<>();
		List<Inbox> inboxes = new ArrayList<>();
		var http = HttpClient.newHttpClient();
		try {
			for (int n = 0; n < 100; n++) {
				Long id = member(UserRole.USER);
				ids.add(id);
				var inbox = new Inbox();
				inboxes.add(inbox);
				sockets.add(
						connect(http, tokens.issue(id).accessToken(), inbox)
								.get(10, TimeUnit.SECONDS));
				assertThat(inbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");
			}
			for (var socket : sockets) {
				socket.sendText("ping", true).get(5, TimeUnit.SECONDS);
			}
			for (var inbox : inboxes) {
				assertThat(inbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("pong");
			}
		} finally {
			sockets.forEach(WebSocket::abort);
			http.shutdownNow();
			ids.forEach(id -> users.deleteById(id));
		}
	}

	@Test
	void 토큰_없는_업그레이드와_온보딩_회원은_거절한다() throws Exception {
		var http = HttpClient.newHttpClient();
		Long onboarding = member(UserRole.ONBOARDING_USER);
		try {
			assertThatThrownBy(() -> connect(http, null, new Inbox()).get(5, TimeUnit.SECONDS))
					.isInstanceOf(ExecutionException.class);
			assertThatThrownBy(
							() ->
									connect(http, "invalid-token", new Inbox())
											.get(5, TimeUnit.SECONDS))
					.isInstanceOf(ExecutionException.class);
			String token = tokens.issue(onboarding).accessToken();
			assertThatThrownBy(() -> connect(http, token, new Inbox()).get(5, TimeUnit.SECONDS))
					.isInstanceOf(ExecutionException.class);
		} finally {
			http.shutdownNow();
			users.deleteById(onboarding);
		}
	}

	private static class Inbox implements WebSocket.Listener {
		final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

		@Override
		public void onOpen(WebSocket socket) {
			socket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
			messages.add(data.toString());
			socket.request(1);
			return CompletableFuture.completedFuture(null);
		}
	}
}
