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
				"insert into graduation_submissions (graduation_id, video_object_key, submitted_at, created_at, status, reviewed_at, review_note, retake_instructions, retake_image_url) values (1, 'old', now(), now(), 'REVISION_REQUESTED', now(), '보완', '[\"전신\"]', 'image')");
		Flyway.configure().dataSource(source).load().migrate();
		assertThat(jdbc.queryForObject("select to_regclass('topic_graduations')::text", String.class)).isNull();
		assertThat(jdbc.queryForList("select status from graduation_submissions order by topic_id", String.class))
				.containsExactly("REVISION_REQUESTED", "APPROVED", "SUBMITTED");
		assertThat(jdbc.queryForObject("select review ->> 'note' from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("보완");
		assertThat(jdbc.queryForObject(
				"select review -> 'retakeInstructions' ->> 0 from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("전신");
		assertThat(jdbc.queryForObject("select retake_image_url from graduation_submissions where topic_id = 1",
				String.class)).isEqualTo("image");
		assertThat(jdbc.queryForObject("select reviewed_at is not null from graduation_submissions where topic_id = 2",
				Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions where user_id = 42 and version = 0",
				Integer.class)).isEqualTo(3);
		assertThatThrownBy(() -> jdbc.update(
				"insert into graduation_submissions (user_id, topic_id, video_object_key, submitted_at, created_at, status) values (42, 3, 'duplicate', now(), now(), 'SUBMITTED')"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void 중복_활성_제출이_있으면_삭제하지_않고_이관에_실패한다() {
		var source = source("single_submission_conflict");
		var jdbc = new JdbcTemplate(source);
		parents(jdbc);
		jdbc.update(
				"insert into graduation_submissions (graduation_id, video_object_key, submitted_at, created_at, status) values (3, 'a', now(), now(), 'SUBMITTED'), (3, 'b', now(), now(), 'APPROVED')");
		assertThatThrownBy(() -> Flyway.configure().dataSource(source).load().migrate()).isInstanceOf(
				org.flywaydb.core.api.FlywayException.class);
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from topic_graduations", Integer.class)).isEqualTo(3);
	}

	private DriverManagerDataSource source(String name) {
		var source = new DriverManagerDataSource("jdbc:tc:postgresql:18.3:///" + name + "?TC_DAEMON=true", "test",
				"test");
		Flyway.configure().dataSource(source).target("21").load().migrate();
		return source;
	}

	private void parents(JdbcTemplate jdbc) {
		jdbc.update(
				"insert into topic_graduations (id, user_id, topic_id, status, video_object_key, submitted_at, approved_at, created_at, updated_at) values (1, 42, 1, 'REVISION_REQUESTED', 'old', now(), null, now(), now()), (2, 42, 2, 'APPROVED', 'approved', now(), now(), now(), now()), (3, 42, 3, 'SUBMITTED', 'pending', now(), null, now(), now())");
	}
}
