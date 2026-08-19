package com.daesabu.meongcoach.user.adapter.security;

import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * user 모듈이 시큐리티 필터 체인에 제공하는 빈 정의.
 * UserRoleAuthenticationConverter를 @Component로 두면 `@WebMvcTest` 슬라이스가
 * Converter 타입을 로드 대상에 포함시켜 모든 컨트롤러 슬라이스가 회원 조회 의존성을 요구하게 되므로,
 * 슬라이스 스캔에서 제외되는 @Configuration 빈 정의로 등록한다.
 */
@Configuration
class UserSecurityConfig {

	@Bean
	Converter<Jwt, AbstractAuthenticationToken> userRoleAuthenticationConverter(
			RegisteredUserChecker registeredUserChecker) {
		return new UserRoleAuthenticationConverter(registeredUserChecker);
	}
}
