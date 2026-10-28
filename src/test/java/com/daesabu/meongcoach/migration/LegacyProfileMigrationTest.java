package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LegacyProfileMigrationTest {

	@Test
	void 운영_스키마의_기존_프로필과_온보딩_미완료_회원을_보존한다() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:tc:postgresql:18.3:///legacy_profile_migration?TC_DAEMON=true", "test", "test");
		Flyway.configure().dataSource(dataSource).target("16").load().migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				insert into users (id, role, status, created_at, updated_at)
				values (10, 'USER', 'ACTIVE', current_timestamp, current_timestamp),
				       (20, 'USER', 'ACTIVE', current_timestamp, current_timestamp),
				       (30, 'ONBOARDING_USER', 'ACTIVE', current_timestamp, current_timestamp)
				""");
		jdbc.update("""
				insert into user_profiles
				(user_id, nickname, profile_image_url, mbti, prior_training_topic_ids, training_goal_topic_ids,
				 is_completed_tooltip, created_at, updated_at)
				values (10, '기존보호자', 'https://legacy.example.com/a.jpg', 'INTJ', ARRAY[1,2], ARRAY[3],
				        true, current_timestamp, current_timestamp),
				       (20, '다른보호자', '', 'ENFP', ARRAY[]::bigint[], ARRAY[4],
				        false, current_timestamp, current_timestamp)
				""");
		List<Map<String, Object>> profiles = jdbc.queryForList("""
				select user_id, nickname, profile_image_url, mbti,
				       prior_training_topic_ids::text, training_goal_topic_ids::text,
				       is_completed_tooltip, created_at, updated_at from user_profiles order by user_id
				""");

		Flyway.configure().dataSource(dataSource).load().migrate();

		assertThat(jdbc.queryForList("""
				select u.id as user_id, p.nickname, p.profile_image_url, p.mbti,
				       p.prior_training_topic_ids::text, p.training_goal_topic_ids::text,
				       p.is_completed_tooltip, p.created_at, p.updated_at
				from users u join user_profiles p on p.id = u.profile_id order by u.id
				""")).containsExactlyElementsOf(profiles);
		assertThat(jdbc.queryForObject("select profile_id from users where id = 30", Long.class)).isNull();
		assertThat(jdbc.queryForObject("select role from users where id = 30", String.class))
				.isEqualTo("ONBOARDING_USER");
	}
}
