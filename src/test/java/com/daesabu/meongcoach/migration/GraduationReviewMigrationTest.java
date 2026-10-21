package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GraduationReviewMigrationTest {

	@Test
	void 최신_판정만_해당_차수로_이전하고_과거_판정은_미상으로_남긴다() {
		var dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///graduation_review_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("18").load().migrate();
		var jdbc = new JdbcTemplate(dataSource);
		jdbc.update("insert into topic_graduations (id, user_id, topic_id, status, attempt_no, video_object_key, "
				+ "submitted_at, created_at, updated_at, reviewed_at, review_note, "
				+ "retake_image_url, retake_instructions) values "
				+ "(1, 42, 7, 'REVISION_REQUESTED', 2, 'second.mp4', now(), now(), now(), now(), '보완 요청', "
				+ "'https://images.test/images/graduation-retake/3/example.jpg', '[\"첫째 지시\", \"둘째 지시\"]'::jsonb), "
				+ "(2, 42, 8, 'APPROVED', 1, 'approved.mp4', now(), now(), now(), now(), '승인', null, '[]'::jsonb), "
				+ "(3, 42, 9, 'SUBMITTED', 1, 'pending.mp4', now(), now(), now(), null, null, null, '[]'::jsonb)");
		jdbc.update("insert into graduation_submissions (graduation_id, attempt_no, video_object_key, submitted_at, created_at) "
				+ "values (1, 1, 'first.mp4', now(), now()), (1, 2, 'second.mp4', now(), now())");

		Flyway.configure().dataSource(dataSource).target("19").load().migrate();

		assertThat(jdbc.queryForObject("select status from graduation_submissions where graduation_id = 1 "
				+ "and attempt_no = 1", String.class)).isEqualTo("UNKNOWN");
		assertThat(jdbc.queryForObject("select review_note from graduation_submissions where graduation_id = 1 "
				+ "and attempt_no = 1", String.class)).isNull();
		assertThat(jdbc.queryForObject("select s.review_note from topic_graduations g "
				+ "join graduation_submissions s on s.id = g.current_submission_id where g.id = 1", String.class))
				.isEqualTo("보완 요청");
		assertThat(jdbc.queryForList("select jsonb_array_elements_text(retake_instructions) from graduation_submissions "
				+ "where graduation_id = 1 and attempt_no = 2", String.class)).containsExactly("첫째 지시", "둘째 지시");
		assertThat(jdbc.queryForObject("select retake_image_url from graduation_submissions "
				+ "where graduation_id = 1 and attempt_no = 2", String.class)).isEqualTo("https://images.test/images/graduation-retake/3/example.jpg");
		assertThat(jdbc.queryForObject("select count(*) from topic_graduations g join graduation_submissions s "
				+ "on s.id = g.current_submission_id and s.graduation_id = g.id and s.attempt_no = g.attempt_no "
				+ "and s.status = g.status", Integer.class)).isEqualTo(3);
		assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
				+ "where table_name = 'topic_graduations' and column_name in "
				+ "('review_note', 'reviewed_at', 'retake_instructions', 'retake_image_url')", Integer.class)).isZero();

		Flyway.configure().dataSource(dataSource).target("20").load().migrate();

		assertThat(jdbc.queryForObject("select count(*) from information_schema.columns "
				+ "where table_name = 'topic_graduations' and column_name = 'current_submission_id'", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions", Integer.class)).isEqualTo(4);
		assertThat(jdbc.queryForObject("select s.review_note from graduation_submissions s "
				+ "join topic_graduations g on s.graduation_id = g.id and s.attempt_no = g.attempt_no "
				+ "where g.id = 1", String.class)).isEqualTo("보완 요청");
		assertThat(jdbc.queryForList("select jsonb_array_elements_text(retake_instructions) from graduation_submissions "
				+ "where graduation_id = 1 and attempt_no = 2", String.class)).containsExactly("첫째 지시", "둘째 지시");

	}
}
