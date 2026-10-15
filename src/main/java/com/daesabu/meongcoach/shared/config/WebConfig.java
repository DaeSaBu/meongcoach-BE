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

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final String ADMIN_PATH_PATTERN = "/api/admin/**";

	private final String adminKey;

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

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/swagger-ui/**")
				.addResourceLocations("classpath:/static/swagger-ui/", "file:build/api-spec/");
	}
}
