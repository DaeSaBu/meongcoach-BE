package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.CurrentUserIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 구성. 빈 등록만 담당하고 로직은 shared/security에 둔다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

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
