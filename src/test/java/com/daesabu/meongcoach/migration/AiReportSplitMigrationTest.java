package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiReportSplitMigrationTest {

	private static final String INSERT_LEGACY_REPORT = "insert into ai_reports "
			+ "(user_id, video_object_key, title, content, status, upload_expires_at, created_at) "
			+ "values (?, ?, ?, ?, ?, ?::timestamp, ?::timestamp)";

	@Test
	void 기존_리포트의_id를_보존해_분석_건과_완료_결과로_나눈다() {
		DriverManagerDataSource dataSource = dataSource("ai_report_split_data");
		migrate(dataSource, "43");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update(INSERT_LEGACY_REPORT, 7L, "completed.mp4", "제목", "{\"report\":[]}", "COMPLETED", null, "2026-08-01 09:00:00");
		jdbc.update(INSERT_LEGACY_REPORT, 7L, "untitled.mp4", null, "{\"solution\":[]}", "COMPLETED", "2026-08-02 09:15:00", "2026-08-02 09:00:00");
		jdbc.update(INSERT_LEGACY_REPORT, 7L, "failed.mp4", null, null, "FAILED_ANALYSIS", "2026-08-03 09:15:00", "2026-08-03 09:00:00");
		jdbc.update(INSERT_LEGACY_REPORT, 8L, "uploading.mp4", null, null, "UPLOADING", "2026-08-04 09:15:00", "2026-08-04 09:00:00");

		migrate(dataSource, "44");

		assertThat(jdbc.queryForList("select id, user_id, video_object_key, status, report_id from ai_analyses order by id"))
				.containsExactly(
						Map.of("id", 1L, "user_id", 7L, "video_object_key", "completed.mp4", "status", "COMPLETED", "report_id", 1L),
						Map.of("id", 2L, "user_id", 7L, "video_object_key", "untitled.mp4", "status", "COMPLETED", "report_id", 2L),
						analysisWithoutReport(3L, 7L, "failed.mp4", "FAILED_ANALYSIS"),
						analysisWithoutReport(4L, 8L, "uploading.mp4", "UPLOADING"));
		assertThat(jdbc.queryForObject("select upload_expires_at::text from ai_analyses where id = 4", String.class))
				.isEqualTo("2026-08-04 09:15:00");
		assertThat(jdbc.queryForObject("select upload_expires_at from ai_analyses where id = 1", String.class)).isNull();
		assertThat(jdbc.queryForObject("select title from ai_reports where id = 1", String.class)).isEqualTo("제목");
		assertThat(jdbc.queryForObject("select title from ai_reports where id = 2", String.class)).isNull();
		assertThat(jdbc.queryForObject("select content from ai_reports where id = 1", String.class)).isEqualTo("{\"report\":[]}");
		assertThat(jdbc.queryForObject("select created_at::text from ai_reports where id = 2", String.class))
				.isEqualTo("2026-08-02 09:00:00");
		assertThat(jdbc.queryForObject("select count(*) from ai_reports", Long.class)).isEqualTo(2L);
		assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
				+ "where table_name = 'ai_analyses' and column_name in ('title', 'content')", Long.class)).isZero();

		assertThat(jdbc.queryForObject("insert into ai_analyses (user_id, video_object_key, status, created_at) "
				+ "values (7, 'next.mp4', 'UPLOADING', now()) returning id", Long.class)).isEqualTo(5L);
		assertThat(jdbc.queryForObject("insert into ai_reports (content, created_at) values ('본문', now()) returning id", Long.class))
				.isEqualTo(3L);
	}

	@Test
	void 리포트가_없어도_마이그레이션되고_새_결과의_id는_1부터_시작한다() {
		DriverManagerDataSource dataSource = dataSource("ai_report_split_empty");

		migrate(dataSource, "44");

		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		assertThat(jdbc.queryForObject("insert into ai_reports (content, created_at) values ('본문', now()) returning id", Long.class))
				.isEqualTo(1L);
	}

	@Test
	void 상태와_결과가_맞지_않는_row가_있으면_실패하고_기존_스키마를_유지한다() {
		DriverManagerDataSource dataSource = dataSource("ai_report_split_guard");
		migrate(dataSource, "43");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		jdbc.update(INSERT_LEGACY_REPORT, 7L, "no-content.mp4", "제목", null, "COMPLETED", null, "2026-08-01 09:00:00");
		assertMigrationRejected(dataSource, jdbc);

		jdbc.update("delete from ai_reports");
		jdbc.update(INSERT_LEGACY_REPORT, 7L, "failed-content.mp4", null, "본문", "FAILED_ANALYSIS", null, "2026-08-01 09:00:00");
		assertMigrationRejected(dataSource, jdbc);

		jdbc.update("delete from ai_reports");
		jdbc.update(INSERT_LEGACY_REPORT, 7L, "failed-title.mp4", "제목", null, "FAILED_ANALYSIS", null, "2026-08-01 09:00:00");
		assertMigrationRejected(dataSource, jdbc);
	}

	private static void assertMigrationRejected(DriverManagerDataSource dataSource, JdbcTemplate jdbc) {
		assertThatThrownBy(() -> migrate(dataSource, "44")).isInstanceOf(FlywayException.class);
		assertThat(jdbc.queryForObject("select to_regclass('ai_analyses')::text", String.class)).isNull();
		assertThat(jdbc.queryForObject("select count(*) from ai_reports", Long.class)).isEqualTo(1L);
	}

	private static Map<String, Object> analysisWithoutReport(Long id, Long userId, String videoObjectKey, String status) {
		Map<String, Object> row = new HashMap<>();
		row.put("id", id);
		row.put("user_id", userId);
		row.put("video_object_key", videoObjectKey);
		row.put("status", status);
		row.put("report_id", null);
		return row;
	}

	private static DriverManagerDataSource dataSource(String databaseName) {
		return new DriverManagerDataSource("jdbc:tc:postgresql:18.3:///" + databaseName + "?TC_DAEMON=true", "test", "test");
	}

	private static void migrate(DriverManagerDataSource dataSource, String target) {
		Flyway.configure().dataSource(dataSource).target(target).load().migrate();
	}
}
