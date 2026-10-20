package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GraduationRetakeMigrationTest {

	@Test
	void 기존_재촬영_팁의_순서와_구도_코드를_JSON_컬럼으로_이전한다() {
		var dataSource = new DriverManagerDataSource("jdbc:tc:postgresql:18.3:///graduation_retake_migration", "test", "test");
		Flyway.configure().dataSource(dataSource).target("18").load().migrate();
		var jdbc = new JdbcTemplate(dataSource);
		jdbc.update("insert into topic_graduations (id, user_id, topic_id, status, attempt_no, video_object_key, "
				+ "submitted_at, created_at, updated_at, retake_framing) "
				+ "values (1, 42, 7, 'REVISION_REQUESTED', 1, 'first.mp4', now(), now(), now(), 'ENTRANCE'), "
				+ "(2, 42, 8, 'SUBMITTED', 1, 'second.mp4', now(), now(), now(), null)");
		jdbc.update("insert into graduation_retake_tips values (1, 1, '둘째 지시'), (1, 0, '첫째 지시')");

		Flyway.configure().dataSource(dataSource).target("19").load().migrate();

		assertThat(jdbc.queryForObject("select retake_framing_icon_code from topic_graduations where id = 1", String.class))
				.isEqualTo("ENTRANCE");
		assertThat(jdbc.queryForList("select jsonb_array_elements_text(retake_instructions) "
				+ "from topic_graduations where id = 1", String.class)).containsExactly("첫째 지시", "둘째 지시");
		assertThat(jdbc.queryForObject("select retake_instructions::text from topic_graduations where id = 2", String.class))
				.isEqualTo("[]");
		assertThat(jdbc.queryForObject("select to_regclass('graduation_retake_tips')::text", String.class)).isNull();
	}
}
