package com.daesabu.meongcoach.purchase.adapter.webapi;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * RevenueCat 웹훅 경로에 발신자 확인 인터셉터를 건다. shared/config/WebConfig에 두면 shared가 purchase에 의존하게 되어 여기 둔다.
 * {@code @WebMvcTest}는 모든 WebMvcConfigurer를 불러오지만 @ConfigurationPropertiesScan은 적용하지 않으므로,
 * 설정을 여기서 직접 등록해야 다른 컨트롤러 슬라이스 테스트가 이 빈을 못 찾아 실패하지 않는다.
 */
@Configuration
@EnableConfigurationProperties(RevenueCatWebhookProperties.class)
@RequiredArgsConstructor
public class RevenueCatWebhookConfig implements WebMvcConfigurer {

	private final RevenueCatWebhookProperties properties;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new RevenueCatWebhookAuthInterceptor(properties))
				.addPathPatterns(RevenueCatWebhookController.PATH);
	}
}
