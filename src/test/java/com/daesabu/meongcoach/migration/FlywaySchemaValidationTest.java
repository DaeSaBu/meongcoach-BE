package com.daesabu.meongcoach.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Flyway 마이그레이션이 만든 PostgreSQL 스키마가 현재 JPA 엔티티와 일치하는지 검증한다.
 * dev/prod 기동 경로(Flyway 적용 → ddl-auto validate)를 그대로 재현하므로, 엔티티를 바꾸고 마이그레이션을
 * 빠뜨리면 배포 전에 여기서 실패한다. PostgreSQL이 필요해 CI에서만 켜고 로컬 H2 테스트에서는 건너뛴다.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SCHEMA_CHECK_DB_URL", matches = ".+")
@TestPropertySource(properties = {
		"spring.datasource.url=${SCHEMA_CHECK_DB_URL}",
		"spring.datasource.driver-class-name=org.postgresql.Driver",
		"spring.datasource.username=${SCHEMA_CHECK_DB_USERNAME}",
		"spring.datasource.password=${SCHEMA_CHECK_DB_PASSWORD}",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class FlywaySchemaValidationTest {

	@Test
	void 마이그레이션이_만든_스키마가_엔티티와_일치한다() {
	}
}
