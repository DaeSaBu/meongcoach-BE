package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiTrialFreeCountMigrationTest {

	@Test
	void 기본_부여_횟수를_3회로_줄이고_추가_부여분은_보존하며_사용_횟수는_기본_부여분부터_쓴_것으로_맞춘다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///ai_trial_free_count_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("71").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				insert into ai_trials (user_id, granted_count, used_count, created_at, updated_at)
				values (1, 30, 2, now(), now()),
				       (2, 30, 10, now(), now()),
				       (3, 40, 10, now(), now()),
				       (4, 40, 35, now(), now()),
				       (5, 30, 31, now(), now()),
				       (6, 10, 5, now(), now())
				""");

		Flyway.configure().dataSource(dataSource).target("72").load().migrate();

		assertThat(jdbc.queryForList("select user_id, granted_count, used_count from ai_trials order by user_id"))
				.containsExactly(
						Map.of("user_id", 1L, "granted_count", 3, "used_count", 2),
						Map.of("user_id", 2L, "granted_count", 3, "used_count", 3),
						Map.of("user_id", 3L, "granted_count", 13, "used_count", 3),
						Map.of("user_id", 4L, "granted_count", 13, "used_count", 8),
						Map.of("user_id", 5L, "granted_count", 3, "used_count", 3),
						Map.of("user_id", 6L, "granted_count", 0, "used_count", 0));
		jdbc.update("insert into ai_trials (user_id, created_at, updated_at) values (7, now(), now())");
		assertThat(jdbc.queryForObject("select granted_count from ai_trials where user_id = 7", Integer.class))
				.isEqualTo(3);
	}
}
