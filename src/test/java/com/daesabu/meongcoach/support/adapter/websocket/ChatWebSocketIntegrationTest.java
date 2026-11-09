package com.daesabu.meongcoach.support.adapter.websocket;

import static org.assertj.core.api.Assertions.*;

import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.support.adapter.integration.PostgresChatListener;
import com.daesabu.meongcoach.support.application.provided.ChatRegister;
import com.daesabu.meongcoach.support.application.provided.dto.ChatMessageRequest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
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
@Sql("/db/migration/V50__create_support_chat.sql")
@Sql(
		statements = {
			"DROP TABLE support_socket_tickets",
			"DROP TABLE support_messages",
			"DROP TABLE support_conversations"
		},
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChatWebSocketIntegrationTest {
	@Autowired ChatRegister register;
	@Autowired UserRepository users;
	@Autowired ChatWebSocketHandler handler;
	@Autowired DataSource dataSource;
	@Autowired JdbcTemplate jdbc;
	@Autowired PlatformTransactionManager transactions;
	@LocalServerPort int port;

	@Test
	void 실제_100개_연결에서_커밋_알림과_롤백_격리와_재연결을_검증한다() throws Exception {
		List<Long> ids = new ArrayList<>();
		List<WebSocket> sockets = new ArrayList<>();
		List<Inbox> inboxes = new ArrayList<>();
		var listener = new PostgresChatListener(dataSource, handler);
		var http = HttpClient.newHttpClient();
		try {
			listener.start();
			for (int n = 0; n < 100; n++) {
				Long id = users.save(User.registerUser()).getId();
				ids.add(id);
				register.open(id);
				var inbox = new Inbox();
				inboxes.add(inbox);
				var socket =
						http.newWebSocketBuilder()
								.subprotocols("support.v1", "ticket." + register.issueTicket(id))
								.buildAsync(
										URI.create("ws://localhost:" + port + "/ws/support"), inbox)
								.get(10, TimeUnit.SECONDS);
				sockets.add(socket);
				assertThat(inbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("sync");
			}
			assertThat(sockets).hasSize(100);
			for (var socket : sockets) {
				socket.sendText("ping", true).get(5, TimeUnit.SECONDS);
			}
			for (var inbox : inboxes) {
				assertThat(inbox.messages.poll(10, TimeUnit.SECONDS)).isEqualTo("pong");
			}
			var tx = new TransactionTemplate(transactions);
			tx.executeWithoutResult(
					status -> {
						register.send(
								ids.getFirst(),
								ids.getFirst(),
								new ChatMessageRequest(UUID.randomUUID(), "롤백"));
						status.setRollbackOnly();
					});
			assertThat(inboxes.getFirst().messages.poll(300, TimeUnit.MILLISECONDS)).isNull();
			register.send(
					ids.getFirst(),
					ids.getFirst(),
					new ChatMessageRequest(UUID.randomUUID(), "커밋"));
			assertThat(inboxes.getFirst().messages.poll(10, TimeUnit.SECONDS)).isEqualTo("changed");
			assertThat(inboxes.get(1).messages.poll(300, TimeUnit.MILLISECONDS)).isNull();
			var pool = dataSource.unwrap(HikariDataSource.class);
			assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
			Connection connection =
					(Connection) ReflectionTestUtils.getField(listener, "connection");
			int pid;
			try (var statement = connection.createStatement();
					var result = statement.executeQuery("SELECT pg_backend_pid()")) {
				result.next();
				pid = result.getInt(1);
			}
			jdbc.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pid);
			assertThat(inboxes.getFirst().messages.poll(15, TimeUnit.SECONDS)).isEqualTo("sync");
			assertThatThrownBy(
							() ->
									http.newWebSocketBuilder()
											.subprotocols("support.v1")
											.buildAsync(
													URI.create(
															"ws://localhost:"
																	+ port
																	+ "/ws/support"),
													new Inbox())
											.get(5, TimeUnit.SECONDS))
					.isInstanceOf(ExecutionException.class);
		} finally {
			sockets.forEach(WebSocket::abort);
			http.shutdownNow();
			listener.stop();
			jdbc.update("DELETE FROM support_messages");
			jdbc.update("DELETE FROM support_socket_tickets");
			jdbc.update("DELETE FROM support_conversations");
			ids.forEach(id -> users.deleteById(id));
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
