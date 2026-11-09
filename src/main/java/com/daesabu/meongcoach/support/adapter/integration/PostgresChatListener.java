package com.daesabu.meongcoach.support.adapter.integration;

import com.daesabu.meongcoach.support.adapter.websocket.ChatWebSocketHandler;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class PostgresChatListener {
	private final DataSource dataSource;
	private final ChatWebSocketHandler sockets;
	private volatile boolean running;
	private volatile Connection connection;
	private Thread worker;

	@EventListener(ApplicationReadyEvent.class)
	public void start() {
		running = true;
		worker = Thread.ofPlatform().daemon().name("support-postgres-listener").start(this::listen);
	}

	private void listen() {
		while (running) {
			try {
				HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
				var properties = new java.util.Properties();
				properties.putAll(pool.getDataSourceProperties());
				properties.setProperty("user", pool.getUsername());
				properties.setProperty("password", pool.getPassword());
				properties.setProperty("connectTimeout", "5");
				properties.setProperty("tcpKeepAlive", "true");
				try (Connection active =
						DriverManager.getConnection(pool.getJdbcUrl(), properties)) {
					connection = active;
					active.setAutoCommit(true);
					try (var statement = active.createStatement()) {
						statement.execute("LISTEN support_chat");
					}
					sockets.synchronizeAll();
					PGConnection postgres = active.unwrap(PGConnection.class);
					Instant syncAt = Instant.now().plusSeconds(60);
					while (running) {
						PGNotification[] notifications = postgres.getNotifications(1000);
						if (notifications != null) {
							for (PGNotification notification : notifications) {
								try {
									sockets.changed(Long.parseLong(notification.getParameter()));
								} catch (NumberFormatException exception) {
									log.warn("상담 알림 형식 오류");
								}
							}
						}
						if (Instant.now().isAfter(syncAt)) {
							sockets.synchronizeAll();
							syncAt = Instant.now().plusSeconds(60);
						}
					}
				}
			} catch (SQLException exception) {
				if (running) {
					log.warn("상담 알림 연결 재시도: {}", exception.getSQLState());
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			} finally {
				connection = null;
			}
		}
	}

	@PreDestroy
	public void stop() {
		running = false;
		if (worker != null) {
			worker.interrupt();
		}
		Connection active = connection;
		if (active != null) {
			try {
				active.close();
			} catch (SQLException exception) {
				log.warn("상담 알림 연결 종료 실패");
			}
		}
	}
}
