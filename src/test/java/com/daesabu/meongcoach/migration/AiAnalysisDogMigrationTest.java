package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiAnalysisDogMigrationTest {

	@Test
	void 분석_건을_선택된_강아지로_옮기고_강아지가_없는_사용자의_분석_건과_리포트는_지운다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///ai_analysis_dog_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("66").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				insert into dogs (id, user_id, name, breed, sex, weight_kg, status, profile_image_url, expectation,
				                  deleted_at, created_at, updated_at)
				values (10, 1, '첫째', '믹스', 'MALE', 4.5, 'UNSELECTED', '', '', null, now(), now()),
				       (11, 1, '둘째', '믹스', 'MALE', 4.5, 'SELECTED', '', '', null, now(), now()),
				       (20, 2, '삭제된 강아지', '믹스', 'MALE', 4.5, 'SELECTED', '', '', now(), now(), now())
				""");
		jdbc.update("""
				insert into ai_reports (id, title, content, created_at)
				values (100, '선택된 강아지', '{}', now()),
				       (300, '강아지 없음', '{}', now())
				""");
		jdbc.update("""
				insert into ai_analyses (user_id, video_object_key, status, report_id, created_at)
				values (1, 'selected', 'COMPLETED', 100, now()),
				       (2, 'deleted-dog', 'UPLOADING', null, now()),
				       (3, 'dogless', 'COMPLETED', 300, now()),
				       (3, 'dogless-uploading', 'UPLOADING', null, now())
				""");

		Flyway.configure().dataSource(dataSource).target("67").load().migrate();

		assertThat(jdbc.queryForList("select user_id, dog_id, video_object_key from ai_analyses order by user_id"))
				.containsExactly(
						Map.of("user_id", 1L, "dog_id", 11L, "video_object_key", "selected"),
						Map.of("user_id", 2L, "dog_id", 20L, "video_object_key", "deleted-dog"));
		assertThat(jdbc.queryForList("select id from ai_reports order by id", Long.class)).containsExactly(100L);
	}
}
