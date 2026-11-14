package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:tc:postgresql:18.3:///meongcoach_schema_check",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class FlywaySchemaValidationTest {

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private DataSource dataSource;

	@Test
	void 활동시각_분리는_기존_정렬시각을_보존한다() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA chat_activity_test");
				statement.execute("SET LOCAL search_path TO chat_activity_test");
				statement.execute("CREATE TABLE support_conversations (id BIGINT, updated_at TIMESTAMP)");
				statement.execute("CREATE INDEX support_conversations_activity ON support_conversations (updated_at DESC, id DESC)");
				statement.execute("INSERT INTO support_conversations VALUES (1, '2026-11-01T01:00:00.123456')");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V60__separate_support_message_activity.sql"));
				try (var rows = statement.executeQuery("SELECT last_message_at = updated_at FROM support_conversations")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getBoolean(1)).isTrue();
				}
			} finally {
				connection.rollback();
			}
		}
	}

	@Test
	void 상담_시각을_UTC_LocalDateTime으로_이관한다() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA chat_time_test");
				statement.execute("SET LOCAL search_path TO chat_time_test");
				statement.execute("SET LOCAL TIME ZONE 'Asia/Seoul'");
				statement.execute("CREATE TABLE support_conversations (created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ, deleted_at TIMESTAMPTZ)");
				statement.execute("CREATE TABLE support_messages (created_at TIMESTAMPTZ)");
				statement.execute("INSERT INTO support_conversations VALUES ('2026-11-01T09:00:00.123456+09:00', '2026-11-01T10:00:00+09:00', NULL)");
				statement.execute("INSERT INTO support_messages VALUES ('2026-11-01T09:00:00.123456+09:00')");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V59__align_support_timestamps.sql"));
				try (var rows = statement.executeQuery("SELECT created_at, updated_at, deleted_at FROM support_conversations")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getObject(1, java.time.LocalDateTime.class)).isEqualTo(java.time.LocalDateTime.parse("2026-11-01T00:00:00.123456"));
					assertThat(rows.getObject(2, java.time.LocalDateTime.class)).isEqualTo(java.time.LocalDateTime.parse("2026-11-01T01:00:00"));
					assertThat(rows.getObject(3)).isNull();
				}
				try (var rows = statement.executeQuery("SELECT created_at FROM support_messages")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getObject(1, java.time.LocalDateTime.class)).isEqualTo(java.time.LocalDateTime.parse("2026-11-01T00:00:00.123456"));
				}
			} finally {
				connection.rollback();
			}
		}
	}

	@Test
	void 상담_종료를_도입해도_기존_메시지를_보존하고_활성_상담만_유일하다() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA chat_soft_delete_test");
				statement.execute("SET LOCAL search_path TO chat_soft_delete_test");
				statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
				statement.execute("INSERT INTO users (id) VALUES (42)");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V52__create_support_chat.sql"));
				statement.execute("INSERT INTO support_conversations (id, updated_at) VALUES (42, '2026-11-02T00:00:00Z')");
				statement.execute("INSERT INTO support_messages (conversation_id, sender_id, operator, client_message_id, body, created_at) "
						+ "VALUES (42, 42, false, 'cc23e1d4-c105-435e-9473-1f5e995967c1', '보관할 문의', '2026-11-01T00:00:00Z')");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V57__separate_support_conversation_member.sql"));
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V58__soft_delete_support_conversations.sql"));
				try (var rows = statement.executeQuery("SELECT created_at, updated_at, deleted_at FROM support_conversations WHERE id = 42")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getTimestamp(1).toInstant()).isEqualTo(java.time.Instant.parse("2026-11-01T00:00:00Z"));
					assertThat(rows.getTimestamp(2).toInstant()).isEqualTo(java.time.Instant.parse("2026-11-02T00:00:00Z"));
					assertThat(rows.getTimestamp(3)).isNull();
				}
				statement.execute("UPDATE support_conversations SET deleted_at = now() WHERE id = 42");
				statement.execute("INSERT INTO support_conversations (member_id, created_at) VALUES (42, now())");
				try (var rows = statement.executeQuery("SELECT count(*) FROM support_messages WHERE conversation_id = 42")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getLong(1)).isEqualTo(1);
				}
				assertThatThrownBy(() -> statement.execute("INSERT INTO support_conversations (member_id, created_at) VALUES (42, now())"))
						.isInstanceOf(java.sql.SQLException.class)
						.satisfies(exception -> assertThat(((java.sql.SQLException) exception).getSQLState()).isEqualTo("23505"));
			} finally {
				connection.rollback();
			}
		}
	}

	@Test
	void 대화_ID를_분리해도_기존_소유자와_메시지는_유지하고_회원별_중복은_거절한다() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var statement = connection.createStatement()) {
				statement.execute("CREATE SCHEMA chat_migration_test");
				statement.execute("SET LOCAL search_path TO chat_migration_test");
				statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
				statement.execute("INSERT INTO users (id) VALUES (42), (99)");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V52__create_support_chat.sql"));
				statement.execute("INSERT INTO support_conversations (id) VALUES (42)");
				statement.execute("INSERT INTO support_messages (conversation_id, sender_id, operator, client_message_id, body) "
						+ "VALUES (42, 42, false, 'cc23e1d4-c105-435e-9473-1f5e995967c1', '문의')");
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V57__separate_support_conversation_member.sql"));
				try (var rows = statement.executeQuery("SELECT c.id, c.member_id, m.conversation_id FROM support_conversations c JOIN support_messages m ON c.id = m.conversation_id")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getLong(1)).isEqualTo(42);
					assertThat(rows.getLong(2)).isEqualTo(42);
					assertThat(rows.getLong(3)).isEqualTo(42);
				}
				try (var rows = statement.executeQuery("INSERT INTO support_conversations (member_id) VALUES (99) RETURNING id, member_id")) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getLong(1)).isEqualTo(43);
					assertThat(rows.getLong(2)).isEqualTo(99);
				}
				assertThatThrownBy(() -> statement.execute("INSERT INTO support_conversations (member_id) VALUES (99)"))
						.isInstanceOf(java.sql.SQLException.class)
						.satisfies(exception -> assertThat(((java.sql.SQLException) exception).getSQLState()).isEqualTo("23505"));
			} finally {
				connection.rollback();
			}
		}
	}

	@Test
	void 강아지별_활성_플랜_제약을_변경해도_기존_플랜_상태는_유지한다() throws Exception {
		String insert = "INSERT INTO plans (user_id, dog_id, title, goal, status, created_at) "
				+ "VALUES (955636, ?, ?, '검증', ?, CURRENT_TIMESTAMP)";
		try {
			for (long dogId = 95563601L; dogId <= 95563604L; dogId++) {
				jdbc.sql("INSERT INTO dogs (id, user_id, name, breed, sex, status, profile_image_url, expectation, created_at, updated_at) "
						+ "VALUES (?, 955636, '검증', 'MIXED', 'MALE', 'UNSELECTED', '', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
						.param(dogId).update();
			}
			jdbc.sql("UPDATE dogs SET deleted_at = CURRENT_TIMESTAMP WHERE id = 95563604").update();
			jdbc.sql("DROP INDEX IF EXISTS uk_plans_active_user_dog").update();
			jdbc.sql("CREATE UNIQUE INDEX uk_plans_active_user ON plans (user_id) WHERE status = 'ACTIVE'").update();
			jdbc.sql(insert).params(95563601L, "이전", "REPLACED").update();
			jdbc.sql(insert).params(95563601L, "최신 교체", "REPLACED").update();
			jdbc.sql(insert).params(95563602L, "활성", "ACTIVE").update();
			jdbc.sql(insert).params(95563603L, "교체", "REPLACED").update();
			jdbc.sql(insert).params(95563603L, "완료", "COMPLETED").update();
			jdbc.sql(insert).params(95563604L, "삭제된 강아지", "REPLACED").update();
			try (Connection connection = dataSource.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V43__scope_active_plans_to_dogs.sql"));
			}

			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'ACTIVE' ORDER BY dog_id")
					.query(String.class).list()).containsExactly("활성");
			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'REPLACED' ORDER BY id")
					.query(String.class).list()).containsExactly("이전", "최신 교체", "교체", "삭제된 강아지");
			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'COMPLETED'")
					.query(String.class).single()).isEqualTo("완료");
		} finally {
			jdbc.sql("DELETE FROM plans WHERE user_id = 955636").update();
			jdbc.sql("DELETE FROM dogs WHERE user_id = 955636").update();
			try (Connection connection = dataSource.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V43__scope_active_plans_to_dogs.sql"));
			}
		}
	}

	@Test
	void 활성_플랜은_사용자별_강아지별로_하나만_허용한다() {
		String insert = "INSERT INTO plans (user_id, dog_id, title, goal, status, created_at) "
				+ "VALUES (955635, ?, '검증', '검증', 'ACTIVE', CURRENT_TIMESTAMP)";
		try {
			jdbc.sql(insert).param(1L).update();
			jdbc.sql(insert).param(2L).update();
			assertThatThrownBy(() -> jdbc.sql(insert).param(1L).update())
					.isInstanceOf(DataIntegrityViolationException.class);
		} finally {
			jdbc.sql("DELETE FROM plans WHERE user_id = 955635").update();
		}
	}

	@Test
	void 마이그레이션이_만든_스키마가_엔티티와_일치한다() {
	}

	@Test
	void 이전_등록_토큰_테이블은_없고_FID_테이블만_남는다() {
		Boolean legacyAbsent = jdbc.sql("SELECT to_regclass('public.device_tokens') IS NULL")
				.query(Boolean.class).single();
		Boolean fidPresent = jdbc.sql("SELECT to_regclass('public.device_installations') IS NOT NULL")
				.query(Boolean.class).single();

		assertThat(legacyAbsent).isTrue();
		assertThat(fidPresent).isTrue();
	}
}
