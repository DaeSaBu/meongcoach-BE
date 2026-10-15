package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:tc:postgresql:18.3:///meongcoach_schema_check",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class TrainingPlanActiveIndexTest {

	private static final Long USER_ID = 990001L;
	private static final String INSERT_PLAN = """
			INSERT INTO training_plans (user_id, dog_id, status, mode, generator, title, goal, reason, duration_weeks,
				daily_minutes, days_per_week, started_on, target_end_on, interview_json, created_at, updated_at)
			VALUES (?, 1, ?, 'MANUAL', 'MANUAL', '제목', '목표', '이유', 2, 10, 3, DATE '2026-10-15', DATE '2026-10-28',
				'{}', NOW(), NOW())
			""";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		jdbcTemplate.update("DELETE FROM training_plans WHERE user_id = ?", USER_ID);
	}

	@Test
	void 같은_사용자의_진행_중_플랜을_둘_저장하면_부분_유니크_인덱스가_막는다() {
		jdbcTemplate.update(INSERT_PLAN, USER_ID, "ACTIVE");

		assertThatThrownBy(() -> jdbcTemplate.update(INSERT_PLAN, USER_ID, "ACTIVE"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 진행_중이_아닌_플랜은_여러_개_저장할_수_있다() {
		jdbcTemplate.update(INSERT_PLAN, USER_ID, "ACTIVE");
		jdbcTemplate.update(INSERT_PLAN, USER_ID, "ABANDONED");
		jdbcTemplate.update(INSERT_PLAN, USER_ID, "ABANDONED");

		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM training_plans WHERE user_id = ?",
				Integer.class, USER_ID);
		assertThat(count).isEqualTo(3);
	}
}
