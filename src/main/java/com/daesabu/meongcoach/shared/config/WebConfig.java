package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.AdminKeyInterceptor;
import com.daesabu.meongcoach.shared.security.CurrentUserIdArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 구성. 빈 등록만 담당하고 로직은 shared/security에 둔다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final String ADMIN_PATH_PATTERN = "/api/admin/**";

	private final String adminKey;

	// @WebMvcTest 슬라이스는 @ConfigurationProperties를 올리지 않으므로 키는 @Value로 받는다. 비어 있으면 관리자 경로가 404로 닫힌다
	public WebConfig(@Value("${meongcoach.admin.key:}") String adminKey) {
		this.adminKey = adminKey;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new AdminKeyInterceptor(adminKey)).addPathPatterns(ADMIN_PATH_PATTERN);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentUserIdArgumentResolver());
	}

	// openapi3.json은 빌드 산출물이라 개발 중에는 classpath에 없다. 배포 jar는 classpath(bootJar가 병합)에서,
	// 로컬(IDE Run·bootRun)은 ./gradlew openapi3 산출물에서 읽는다. 접근 제어는 SecurityConfig가 담당한다
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/swagger-ui/**")
				.addResourceLocations("classpath:/static/swagger-ui/", "file:build/api-spec/");
	}
}
