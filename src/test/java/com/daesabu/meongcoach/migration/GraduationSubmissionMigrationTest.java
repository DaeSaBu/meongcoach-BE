package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GraduationSubmissionMigrationTest {
	@Test
	void 판정과_부모만_있는_상태를_보존하고_부모를_제거한다() {
		var source = source("single_submission");
		var jdbc = new JdbcTemplate(source);
		parents(jdbc);
		jdbc.update(
				"insert into graduation_submissions (graduation_id, attempt_no, video_object_key, submitted_at, created_at) values (1, 1, 'old', now(), now())");
		Flyway.configure().dataSource(source).load().migrate();
		assertThat(jdbc.queryForObject("select to_regclass('topic_graduations')::text", String.class)).isNull();
		assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
				+ "where table_name = 'graduation_submissions' and column_name in ('graduation_id', 'attempt_no', "
				+ "'review_note', 'retake_instructions')", Integer.class)).isZero();
		assertThat(jdbc.queryForList("select status from graduation_submissions order by topic_id", String.class))
				.containsExactly("REVISION_REQUESTED", "APPROVED", "SUBMITTED");
		assertThat(jdbc.queryForObject("select review ->> 'note' from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("보완");
		assertThat(jdbc.queryForObject(
				"select review -> 'retakeInstructions' ->> 0 from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("전신");
		assertThat(jdbc.queryForObject("select retake_image_url from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("image");
		assertThat(jdbc.queryForObject("select review ->> 'note' from graduation_submissions where topic_id = 2",
				String.class)).isEqualTo("승인 메모");
		assertThat(jdbc.queryForObject("select reviewed_at is not null from graduation_submissions where topic_id = 2",
				Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions where user_id = 42 and version = 0",
				Integer.class)).isEqualTo(3);
		assertThatThrownBy(() -> jdbc.update(
				"insert into graduation_submissions (user_id, topic_id, video_object_key, submitted_at, created_at, status) values (42, 3, 'duplicate', now(), now(), 'SUBMITTED')"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void 여러_차수_이력은_최신_차수에만_부모_상태를_옮기고_과거_차수는_보완_요청으로_복원한다() {
		var source = source("single_submission_history");
		var jdbc = new JdbcTemplate(source);
		parents(jdbc);
		jdbc.update(
				"insert into graduation_submissions (graduation_id, attempt_no, video_object_key, submitted_at, created_at) values (3, 1, 'a', now(), now()), (3, 2, 'b', now(), now())");
		Flyway.configure().dataSource(source).load().migrate();
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions where user_id = 42 and topic_id = 3",
				Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject(
				"select status from graduation_submissions where video_object_key = 'a'", String.class))
				.isEqualTo("REVISION_REQUESTED");
		assertThat(jdbc.queryForObject(
				"select reviewed_at is null and retake_image_url is null from graduation_submissions where video_object_key = 'a'",
				Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject(
				"select review::text from graduation_submissions where video_object_key = 'a'", String.class))
				.isEqualTo("{\"note\": null, \"retakeInstructions\": []}");
		assertThat(jdbc.queryForObject(
				"select status from graduation_submissions where video_object_key = 'b'", String.class))
				.isEqualTo("SUBMITTED");
		assertThat(jdbc.queryForObject(
				"select video_object_key from graduation_submissions where user_id = 42 and topic_id = 3 order by id desc limit 1",
				String.class)).isEqualTo("b");
	}

	private DriverManagerDataSource source(String name) {
		var source = new DriverManagerDataSource("jdbc:tc:postgresql:18.3:///" + name + "?TC_DAEMON=true", "test",
				"test");
		Flyway.configure().dataSource(source).target("20").load().migrate();
		return source;
	}

	private void parents(JdbcTemplate jdbc) {
		jdbc.update(
				"insert into topic_graduations (id, user_id, topic_id, status, attempt_no, video_object_key, submitted_at, approved_at, created_at, updated_at) values (1, 42, 1, 'REVISION_REQUESTED', 1, 'old', now(), null, now(), now()), (2, 42, 2, 'APPROVED', 1, 'approved', now(), now(), now(), now()), (3, 42, 3, 'SUBMITTED', 2, 'pending', now(), null, now(), now())");
		jdbc.update("update topic_graduations set reviewed_at = now(), review_note = '보완', "
				+ "retake_instructions = '[\"전신\"]'::jsonb, retake_image_url = 'image' where id = 1");
		jdbc.update("update topic_graduations set review_note = '승인 메모' where id = 2");
	}
}
