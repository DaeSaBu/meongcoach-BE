package com.daesabu.meongcoach.shared.webapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * 액세스 토큰으로 인증된 회원을 흉내 내 {@code @LoginUser} 파라미터를 해석할 수 있게 한다.
 * 컨트롤러 슬라이스에는 시큐리티 필터 체인이 없어(test-convention.md) {@code SecurityMockMvcRequestPostProcessors.jwt()}가
 * SecurityContext를 채우지 못하므로, 컨텍스트를 직접 세우는 이 방식을 쓴다. 정리는 스프링 시큐리티 테스트 리스너가 맡는다.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithLoginUser.Factory.class)
public @interface WithLoginUser {

	/**
	 * 액세스 토큰의 sub 클레임. 회원 ID다.
	 */
	String value() default "42";

	class Factory implements WithSecurityContextFactory<WithLoginUser> {

		@Override
		public SecurityContext createSecurityContext(WithLoginUser annotation) {
			Jwt accessToken = Jwt.withTokenValue("access-token")
					.header("alg", "HS256")
					.subject(annotation.value())
					.build();
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new JwtAuthenticationToken(accessToken));
			return context;
		}
	}
}
