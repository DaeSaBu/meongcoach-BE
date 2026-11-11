package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:tc:postgresql:18.3:///meongcoach_schema_check",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class FlywaySchemaValidationTest {

	@Autowired
	private JdbcClient jdbc;

	@Autowired
	private DataSource dataSource;

	@Test
	void 강아지별_활성_플랜_제약을_변경해도_기존_플랜_상태는_유지한다() throws Exception {
		String insert = "INSERT INTO plans (user_id, dog_id, title, goal, status, created_at) "
				+ "VALUES (955636, ?, ?, '검증', ?, CURRENT_TIMESTAMP)";
		try {
			for (long dogId = 95563601L; dogId <= 95563604L; dogId++) {
				jdbc.sql("INSERT INTO dogs (id, user_id, name, breed, sex, status, profile_image_url, expectation, created_at, updated_at) "
						+ "VALUES (?, 955636, '검증', 'MIXED', 'MALE', 'UNSELECTED', '', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
						.param(dogId).update();
			}
			jdbc.sql("UPDATE dogs SET deleted_at = CURRENT_TIMESTAMP WHERE id = 95563604").update();
			jdbc.sql("DROP INDEX IF EXISTS uk_plans_active_user_dog").update();
			jdbc.sql("CREATE UNIQUE INDEX uk_plans_active_user ON plans (user_id) WHERE status = 'ACTIVE'").update();
			jdbc.sql(insert).params(95563601L, "이전", "REPLACED").update();
			jdbc.sql(insert).params(95563601L, "최신 교체", "REPLACED").update();
			jdbc.sql(insert).params(95563602L, "활성", "ACTIVE").update();
			jdbc.sql(insert).params(95563603L, "교체", "REPLACED").update();
			jdbc.sql(insert).params(95563603L, "완료", "COMPLETED").update();
			jdbc.sql(insert).params(95563604L, "삭제된 강아지", "REPLACED").update();
			try (Connection connection = dataSource.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V43__scope_active_plans_to_dogs.sql"));
			}

			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'ACTIVE' ORDER BY dog_id")
					.query(String.class).list()).containsExactly("활성");
			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'REPLACED' ORDER BY id")
					.query(String.class).list()).containsExactly("이전", "최신 교체", "교체", "삭제된 강아지");
			assertThat(jdbc.sql("SELECT title FROM plans WHERE user_id = 955636 AND status = 'COMPLETED'")
					.query(String.class).single()).isEqualTo("완료");
		} finally {
			jdbc.sql("DELETE FROM plans WHERE user_id = 955636").update();
			jdbc.sql("DELETE FROM dogs WHERE user_id = 955636").update();
			try (Connection connection = dataSource.getConnection()) {
				ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V43__scope_active_plans_to_dogs.sql"));
			}
		}
	}

	@Test
	void 활성_플랜은_사용자별_강아지별로_하나만_허용한다() {
		String insert = "INSERT INTO plans (user_id, dog_id, title, goal, status, created_at) "
				+ "VALUES (955635, ?, '검증', '검증', 'ACTIVE', CURRENT_TIMESTAMP)";
		try {
			jdbc.sql(insert).param(1L).update();
			jdbc.sql(insert).param(2L).update();
			assertThatThrownBy(() -> jdbc.sql(insert).param(1L).update())
					.isInstanceOf(DataIntegrityViolationException.class);
		} finally {
			jdbc.sql("DELETE FROM plans WHERE user_id = 955635").update();
		}
	}

	@Test
	void 마이그레이션이_만든_스키마가_엔티티와_일치한다() {
	}

	@Test
	void 이전_등록_토큰_테이블은_없고_FID_테이블만_남는다() {
		Boolean legacyAbsent = jdbc.sql("SELECT to_regclass('public.device_tokens') IS NULL")
				.query(Boolean.class).single();
		Boolean fidPresent = jdbc.sql("SELECT to_regclass('public.device_installations') IS NOT NULL")
				.query(Boolean.class).single();

		assertThat(legacyAbsent).isTrue();
		assertThat(fidPresent).isTrue();
	}
}
