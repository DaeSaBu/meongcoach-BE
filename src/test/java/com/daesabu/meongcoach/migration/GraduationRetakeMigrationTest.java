package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GraduationRetakeMigrationTest {

	@Test
	void 기존_수료에_빈_JSON_안내를_추가하고_별도_테이블을_만들지_않는다() {
		var dataSource = new DriverManagerDataSource("jdbc:tc:postgresql:18.3:///graduation_retake_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("19").load().migrate();
		var jdbc = new JdbcTemplate(dataSource);
		jdbc.update("insert into topic_graduations (id, user_id, topic_id, status, attempt_no, video_object_key, "
				+ "submitted_at, created_at, updated_at) "
				+ "values (1, 42, 7, 'REVISION_REQUESTED', 1, 'first.mp4', now(), now(), now()), "
				+ "(2, 42, 8, 'SUBMITTED', 1, 'second.mp4', now(), now(), now())");

		Flyway.configure().dataSource(dataSource).target("20").load().migrate();

		assertThat(jdbc.queryForObject("select retake_image_url from topic_graduations where id = 1", String.class))
				.isNull();
		assertThat(jdbc.queryForList("select retake_instructions::text from topic_graduations order by id", String.class))
				.containsExactly("[]", "[]");
		assertThat(jdbc.queryForObject("select data_type from information_schema.columns "
				+ "where table_name = 'topic_graduations' and column_name = 'retake_instructions'", String.class))
				.isEqualTo("jsonb");
		assertThat(jdbc.queryForObject("select to_regclass('graduation_retake_tips')::text", String.class)).isNull();
	}
}
