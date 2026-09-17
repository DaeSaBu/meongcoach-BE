package com.daesabu.meongcoach.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Flyway 마이그레이션이 만든 PostgreSQL 스키마가 현재 JPA 엔티티와 일치하는지 검증한다.
 * dev/prod 기동 경로(Flyway 적용 → ddl-auto validate)를 그대로 재현하므로, 엔티티를 바꾸고 마이그레이션을
 * 빠뜨리면 배포 전에 여기서 실패한다. Testcontainers가 PostgreSQL을 띄우므로 로컬·CI 모두 항상 실행된다.
 * 다른 테스트 컨텍스트의 create-drop이 만든 테이블 위에 마이그레이션이 실행되지 않도록 DB 이름이 다른 URL을 써서
 * 별도 컨테이너를 쓴다.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:tc:postgresql:18.3:///meongcoach_schema_check",
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate"
})
class FlywaySchemaValidationTest {

	@Test
	void 마이그레이션이_만든_스키마가_엔티티와_일치한다() {
	}
}
