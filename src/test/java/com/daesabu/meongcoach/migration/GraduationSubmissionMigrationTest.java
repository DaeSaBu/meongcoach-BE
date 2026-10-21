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
	void 판정을_복원할_수_없는_과거_이력이_있으면_삭제하지_않고_이관에_실패한다() {
		var source = source("single_submission_conflict");
		var jdbc = new JdbcTemplate(source);
		parents(jdbc);
		jdbc.update(
				"insert into graduation_submissions (graduation_id, attempt_no, video_object_key, submitted_at, created_at) values (3, 1, 'a', now(), now()), (3, 2, 'b', now(), now())");
		assertThatThrownBy(() -> Flyway.configure().dataSource(source).load().migrate()).isInstanceOf(
				org.flywaydb.core.api.FlywayException.class);
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from topic_graduations", Integer.class)).isEqualTo(3);
	}

	@Test
	void 학습만_완료한_회원은_미제출로_이관하고_미완료와_기존_제출은_중복하지_않는다() {
		var source = source("completed_without_submission");
		var jdbc = new JdbcTemplate(source);
		parents(jdbc);
		jdbc.update("insert into training_categories (id, title, description, icon_url, sort_order, created_at, updated_at) "
				+ "values (1, '교육', '', '', 1, now(), now())");
		jdbc.update("insert into topics (id, training_category_id, title, description, detail, icon_url, sort_order, created_at, updated_at) "
				+ "values (1, 1, '기본', '', '', '', 1, now(), now())");
		jdbc.update("insert into curriculums (id, topic_id, title, thumbnail_url, description, sort_order, created_at, updated_at, is_premium) "
				+ "values (1, 1, '기본', '', '', 1, now(), now(), false)");
		jdbc.update("insert into lessons (id, curriculum_id, title, sort_order, estimated_minutes, created_at, updated_at) "
				+ "values (1, 1, '앉아', 1, 5, now(), now()), (2, 1, '기다려', 2, 5, now(), now())");
		jdbc.update("insert into lesson_progress (user_id, lesson_id, completed_count, created_at, updated_at) "
				+ "values (42, 1, 1, now(), now()), (42, 2, 1, now(), now()), "
				+ "(43, 1, 1, now(), now()), (43, 2, 1, now(), now()), (44, 1, 1, now(), now())");

		Flyway.configure().dataSource(source).load().migrate();

		assertThat(jdbc.queryForList("select user_id from graduation_submissions where status = 'NOT_SUBMITTED'", Long.class))
				.containsExactly(43L);
		assertThat(jdbc.queryForObject("select video_object_key is null and submitted_at is null "
				+ "from graduation_submissions where user_id = 43", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from graduation_submissions where user_id = 42", Integer.class))
				.isEqualTo(3);
		assertThatThrownBy(() -> jdbc.update("insert into graduation_submissions (user_id, topic_id, status, created_at) "
				+ "values (43, 1, 'NOT_SUBMITTED', now())"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
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
