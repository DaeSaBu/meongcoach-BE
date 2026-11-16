package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ProgressMigrationTest {
	@Test
	void 삭제된_레슨의_진행이_있어도_기존_선택과_레슨_진행을_보존한다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///progress_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("22").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				insert into training_categories (id, title, description, icon_url, sort_order, created_at, updated_at)
				values (1, '교육', '', '', 1, current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into topics (id, training_category_id, title, description, detail, icon_url, sort_order, created_at, updated_at)
				select id, 1, '주제', '', '', '', id, current_timestamp, current_timestamp
				from generate_series(1, 2) id
				""");
		jdbc.update("""
				insert into curriculums (id, topic_id, title, sort_order, thumbnail_url, description, is_premium, created_at, updated_at)
				select id, id, '과정', 1, '', '', false, current_timestamp, current_timestamp
				from generate_series(1, 2) id
				""");
		jdbc.update("""
				insert into lessons (id, curriculum_id, title, sort_order, estimated_minutes, created_at, updated_at)
				values (10, 1, '레슨', 1, 5, current_timestamp, current_timestamp),
				       (11, 1, '레슨', 2, 5, current_timestamp, current_timestamp),
				       (20, 2, '레슨', 1, 5, current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into topic_progress (user_id, topic_id, created_at, updated_at)
				values (1, 2, current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into lesson_progress (user_id, lesson_id, completed_count, created_at, updated_at)
				values (1, 10, 3, current_timestamp, current_timestamp),
				       (1, 11, 0, current_timestamp, current_timestamp),
				       (1, 20, 2, current_timestamp, current_timestamp),
				       (2, 10, 1, current_timestamp, current_timestamp)
				""");

		jdbc.update("delete from lessons where id = 20");
		List<Map<String, Object>> records = jdbc.queryForList(
				"select id, user_id, lesson_id, completed_count, created_at, updated_at from lesson_progress order by id");

		Flyway.configure().dataSource(dataSource).target("64").load().migrate();

		assertThat(jdbc.queryForObject("select topic_id from topic_selection where user_id = 1", Long.class))
				.isEqualTo(2L);
		assertThat(jdbc.queryForList(
				"select id, user_id, lesson_id, completed_count, created_at, updated_at from lesson_progress order by id"))
				.containsExactlyElementsOf(records);
		assertThat(jdbc.queryForObject("""
				select count(*) from information_schema.tables
				where table_schema = 'public' and table_name = 'topic_progress'
				""", Integer.class)).isZero();
		assertThat(jdbc.queryForList("""
				select completed_count from lesson_progress where user_id = 1 order by lesson_id
				""", Integer.class)).containsExactly(3, 0, 2);
	}

	@Test
	void 진행과_선택을_선택된_강아지로_옮기고_강아지가_없는_사용자의_기록은_지운다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///progress_dog_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("64").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				insert into dogs (id, user_id, name, breed, sex, weight_kg, status, profile_image_url, expectation,
				                  deleted_at, created_at, updated_at)
				values (10, 1, '첫째', '믹스', 'MALE', 4.5, 'UNSELECTED', '', '', null, current_timestamp, current_timestamp),
				       (11, 1, '둘째', '믹스', 'MALE', 4.5, 'SELECTED', '', '', null, current_timestamp, current_timestamp),
				       (20, 2, '삭제된 강아지', '믹스', 'MALE', 4.5, 'SELECTED', '', '', current_timestamp,
				        current_timestamp, current_timestamp),
				       (21, 2, '남은 강아지', '믹스', 'MALE', 4.5, 'UNSELECTED', '', '', null, current_timestamp,
				        current_timestamp)
				""");
		jdbc.update("""
				insert into lesson_progress (user_id, lesson_id, completed_count, created_at, updated_at)
				values (1, 100, 3, current_timestamp, current_timestamp),
				       (2, 100, 2, current_timestamp, current_timestamp),
				       (3, 100, 1, current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into topic_selection (user_id, topic_id, created_at, updated_at)
				values (1, 7, current_timestamp, current_timestamp),
				       (2, 8, current_timestamp, current_timestamp),
				       (3, 9, current_timestamp, current_timestamp)
				""");

		Flyway.configure().dataSource(dataSource).target("65").load().migrate();

		assertThat(jdbc.queryForList(
				"select user_id, dog_id, completed_count from lesson_progress order by user_id"))
				.containsExactly(
						Map.of("user_id", 1L, "dog_id", 11L, "completed_count", 3),
						Map.of("user_id", 2L, "dog_id", 20L, "completed_count", 2));
		assertThat(jdbc.queryForList("select user_id, dog_id, topic_id from topic_selection order by user_id"))
				.containsExactly(
						Map.of("user_id", 1L, "dog_id", 11L, "topic_id", 7L),
						Map.of("user_id", 2L, "dog_id", 20L, "topic_id", 8L));
	}

	@Test
	void 같은_사용자의_강아지마다_진행과_선택을_따로_저장할_수_있게_유니크_제약을_바꾼다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///progress_dog_constraint_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("65").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		jdbc.update("""
				insert into lesson_progress (user_id, dog_id, lesson_id, completed_count, created_at, updated_at)
				values (1, 10, 100, 1, current_timestamp, current_timestamp),
				       (1, 11, 100, 1, current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into topic_selection (user_id, dog_id, topic_id, created_at, updated_at)
				values (1, 10, 7, current_timestamp, current_timestamp),
				       (1, 11, 8, current_timestamp, current_timestamp)
				""");

		assertThat(jdbc.queryForObject("select count(*) from lesson_progress", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select count(*) from topic_selection", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForList("""
				select conname from pg_constraint
				where conrelid in ('lesson_progress'::regclass, 'topic_selection'::regclass) and contype = 'u'
				order by conname
				""", String.class))
				.containsExactly("uk_lesson_progress_user_dog_lesson", "uk_topic_selection_user_dog");
	}
}
