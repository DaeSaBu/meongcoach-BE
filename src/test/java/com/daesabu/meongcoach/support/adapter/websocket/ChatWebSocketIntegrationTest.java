package com.daesabu.meongcoach.support.adapter.websocket;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.auth.application.required.TokenProvider;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.support.adapter.integration.PostgresChatListener;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserRole;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("/db/migration/V51__create_support_conversations.sql")
@Sql(
		statements = {"DROP TABLE support_messages", "DROP TABLE support_conversations"},
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChatWebSocketIntegrationTest {
	@Autowired UserRepository users;
	@Autowired TokenProvider tokens;
	@Autowired ChatRegister register;
	@Autowired ChatWebSocketHandler handler;
	@Autowired DataSource dataSource;
	@Autowired JdbcTemplate jdbc;
	@Autowired PlatformTransactionManager transactions;
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

	@Test
	void 커밋한_메시지만_알리고_LISTEN_연결이_끊기면_sync로_복구한다() throws Exception {
		Long owner = member(UserRole.USER);
		Long other = member(UserRole.USER);
		var listener = new PostgresChatListener(dataSource, handler);
		var http = HttpClient.newHttpClient();
		List<WebSocket> sockets = new ArrayList<>();
		try {
			register.open(owner);
			var ownerInbox = new Inbox();
			var otherInbox = new Inbox();
			sockets.add(
					connect(http, tokens.issue(owner).accessToken(), ownerInbox)
							.get(10, TimeUnit.SECONDS));
			sockets.add(
					connect(http, tokens.issue(other).accessToken(), otherInbox)
							.get(10, TimeUnit.SECONDS));
			assertThat(ownerInbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");
			assertThat(otherInbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");
			listener.start();
			assertThat(ownerInbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");
			assertThat(otherInbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");

			new TransactionTemplate(transactions)
					.executeWithoutResult(
							status -> {
								register.send(
										owner, owner, new ChatMessageRequest(UUID.randomUUID(), "롤백"));
								status.setRollbackOnly();
							});
			assertThat(ownerInbox.messages.poll(300, TimeUnit.MILLISECONDS)).isNull();

			register.send(owner, owner, new ChatMessageRequest(UUID.randomUUID(), "커밋"));
			assertThat(ownerInbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("changed");
			assertThat(otherInbox.messages.poll(300, TimeUnit.MILLISECONDS)).isNull();

			var pool = dataSource.unwrap(HikariDataSource.class);
			assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
			Connection connection = (Connection) ReflectionTestUtils.getField(listener, "connection");
			int pid;
			try (var statement = connection.createStatement();
					var result = statement.executeQuery("SELECT pg_backend_pid()")) {
				result.next();
				pid = result.getInt(1);
			}
			jdbc.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pid);
			assertThat(ownerInbox.messages.poll(15, TimeUnit.SECONDS)).isEqualTo("sync");
		} finally {
			sockets.forEach(WebSocket::abort);
			http.shutdownNow();
			listener.stop();
			jdbc.update("DELETE FROM support_messages");
			jdbc.update("DELETE FROM support_conversations");
			users.deleteById(owner);
			users.deleteById(other);
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
