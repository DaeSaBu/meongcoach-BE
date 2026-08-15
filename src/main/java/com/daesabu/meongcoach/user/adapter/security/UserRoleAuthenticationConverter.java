package com.daesabu.meongcoach.user.adapter.security;

import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import com.daesabu.meongcoach.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 토큰의 sub로 회원 역할을 조회해 ROLE_* 권한을 부여한다. 역할 조회가 등록 여부 확인을 겸하므로
 * 회원 행이 사라진 뒤에도 남아 있는 토큰은 여기서 401로 끝난다.
 * 역할을 JWT 클레임이 아닌 DB에서 읽으므로 온보딩 완료 승격이 토큰 재발급 없이 즉시 반영된다.
 * 회원 조회가 필요해 shared가 아니라 user 모듈에 두고, SecurityConfig는 스프링 타입으로만 받는다.
 * `@WebMvcTest` 슬라이스가 Converter 구현 컴포넌트를 로드 대상에 포함시키므로,
 * 컴포넌트 스캔 대신 {@link UserSecurityConfig}의 빈 정의로 등록해 슬라이스를 오염시키지 않는다.
 */
public class UserRoleAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	// 원인을 자세히 알리면 회원 ID의 존재 여부가 노출되므로 일반화된 문구만 담는다
	private static final String ERROR_DESCRIPTION = "인증 정보가 올바르지 않습니다.";

	private final RegisteredUserChecker registeredUserChecker;

	public UserRoleAuthenticationConverter(RegisteredUserChecker registeredUserChecker) {
		this.registeredUserChecker = registeredUserChecker;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UserRole role = findRole(jwt)
				// AuthenticationException 계열이 아니면 401 대신 500이 되므로 예외 타입이 중요하다
				.orElseThrow(() -> new InvalidBearerTokenException(ERROR_DESCRIPTION));
		// JwtAuthenticationToken의 name은 sub 그대로라 @CurrentUserId 해석이 바뀌지 않는다
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
	}

	// 형식 위반 sub도 미등록과 같은 응답으로 돌린다
	private Optional<UserRole> findRole(Jwt jwt) {
		try {
			return registeredUserChecker.findRole(Long.valueOf(jwt.getSubject()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
