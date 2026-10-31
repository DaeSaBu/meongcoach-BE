package com.daesabu.meongcoach.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
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
