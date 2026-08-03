package com.daesabu.meongcoach.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * 로컬 전용 OpenAPI 스펙 서빙. openapi3.json은 로컬 빌드 산출물이라 jar에 담지 않으므로
 * build/api-spec 디렉토리에서 직접 읽어 Swagger UI에 제공한다.
 * 정확 경로(/openapi3.json)는 ResourceHandler 패턴 매핑이 불가능해 함수형 엔드포인트를 쓴다.
 */
@Configuration
@Profile("local")
public class ApiDocsConfig {

	// bootRun·IDE 실행 모두 작업 디렉토리가 프로젝트 루트라는 전제의 상대 경로다
	private static final String SPEC_FILE = "build/api-spec/openapi3.json";

	@Bean
	RouterFunction<ServerResponse> openApiSpecRoute() {
		return RouterFunctions.route()
				.GET("/openapi3.json", request -> {
					FileSystemResource spec = new FileSystemResource(SPEC_FILE);
					if (!spec.exists()) {
						return ServerResponse.notFound().build();
					}
					return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(spec);
				})
				.build();
	}
}
