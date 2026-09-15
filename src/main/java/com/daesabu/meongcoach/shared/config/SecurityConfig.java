package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.shared.security.CorsProperties;
import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.shared.security.TokenTypeValidator;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 인증·인가 구성. 클라이언트가 네이티브 앱뿐이라 세션·CSRF·폼 로그인이 필요 없고,
 * 자체 발급 JWT를 Bearer 토큰으로 검증하는 무상태 필터 체인을 둔다.
 * CORS도 이 체인 앞단에서 처리해 preflight는 인가 전에 응답되고 401 응답에도 CORS 헤더가 실린다.
 * 이 클래스는 빈 정의만 담고 로직은 shared/security에 두어 커버리지 측정 대상으로 남긴다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// 토큰을 아직 받지 못한 요청만 열어둔다. `/api/auth/**`로 넓히면 이후 추가될 인증 API까지 공개된다
	private static final String[] PERMIT_ALL_PATHS = {
			"/api/health",
			"/api/auth/login/social/**",
			"/api/auth/login/local",
			"/api/auth/token/refresh",
			"/api/auth/logout"
	};

	// 온보딩 중에도 필요한 경로. 이미지 업로드 URL 발급은 /api/onboarding/** 안에 있고, 프로필 이미지 조회만 밖에 있다
	private static final String[] ONBOARDING_ALLOWED_PATHS = {
			"/api/onboarding/**",
			"/api/dogs/profile/image"
	};

	// 스토어 심사관이 온보딩을 마치지 않고 탈퇴할 수 있으므로 탈퇴만 온보딩 중에도 연다.
	// 메서드를 한정해 같은 경로에 나중에 생길 회원 조회·수정이 온보딩 회원에게 열리지 않게 한다
	private static final String WITHDRAW_PATH = "/api/users/me";

	// Swagger UI 정적 파일과 그 안의 openapi3.json이 모두 이 경로 아래에 있다
	private static final String[] API_DOCS_PATHS = {"/swagger-ui/**"};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
	                                        Converter<Jwt, AbstractAuthenticationToken> userRoleAuthenticationConverter,
	                                        AuthenticationEntryPoint authenticationEntryPoint,
	                                        AccessDeniedHandler accessDeniedHandler,
	                                        CorsConfigurationSource corsConfigurationSource,
	                                        @Value("${meongcoach.api-docs.enabled:false}") boolean apiDocsEnabled) {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> {
					auth.requestMatchers(PERMIT_ALL_PATHS).permitAll();
					configureApiDocsAccess(auth, apiDocsEnabled);
					// 먼저 매칭된 규칙이 이기므로 온보딩 허용 경로를 anyRequest보다 앞에 둔다.
					// 역할 어휘는 AuthorityRole이 단일 원천이다 (user 모듈 UserRole이 같은 어휘로 매핑된다)
					auth.requestMatchers(ONBOARDING_ALLOWED_PATHS)
							.hasAnyRole(AuthorityRole.MEMBER.name(), AuthorityRole.ONBOARDING_MEMBER.name());
					auth.requestMatchers(HttpMethod.DELETE, WITHDRAW_PATH)
							.hasAnyRole(AuthorityRole.MEMBER.name(), AuthorityRole.ONBOARDING_MEMBER.name());
					auth.anyRequest().hasRole(AuthorityRole.MEMBER.name());
				})
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(accessTokenDecoder)
								.jwtAuthenticationConverter(userRoleAuthenticationConverter))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.build();
	}

	// 문서 페이지는 local·dev만 연다. authenticated로 흘리면 유효 토큰 소지자가 운영에서
	// 문서를 볼 수 있어 비활성 환경에서는 denyAll로 완전히 막는다
	private void configureApiDocsAccess(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
			boolean apiDocsEnabled) {
		if (apiDocsEnabled) {
			auth.requestMatchers(API_DOCS_PATHS).permitAll();
			return;
		}
		auth.requestMatchers(API_DOCS_PATHS).denyAll();
	}

	// CORS가 시큐리티 체인 안에서 동작하므로, 필터 체인을 추가하면 그 체인에도 .cors(...)를 걸어야 한다
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(properties.allowedOriginPatterns());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// 이메일 로그인(스토어 심사용 테스트 계정)의 비밀번호 대조에 쓴다. domain은 Spring에 의존할 수 없어 해시 문자열만 보관하고,
	// 해싱·대조는 application이 이 빈으로 수행한다
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).build();
	}

	// 액세스·리프레시 디코더를 분리해 각자 용도를 강제한다. @Primary를 두지 않고 주입 지점마다 명시한다.
	// 회원 등록 여부 확인은 역할 부여 컨버터(user 모듈 구현)가 겸하므로 디코더에는 검증기를 붙이지 않는다
	@Bean
	JwtDecoder accessTokenDecoder(JwtProperties properties) {
		return tokenDecoder(properties, TokenType.ACCESS, List.of());
	}

	// 재발급 경로는 회원 확인을 TokenRefreshService가 맡아 별도 에러 코드를 유지하므로 여기서는 붙이지 않는다
	@Bean
	JwtDecoder refreshTokenDecoder(JwtProperties properties) {
		return tokenDecoder(properties, TokenType.REFRESH, List.of());
	}

	private JwtDecoder tokenDecoder(JwtProperties properties, TokenType tokenType,
	                                List<OAuth2TokenValidator<Jwt>> additionalValidators) {
		SecretKey secretKey = properties.secretKey();
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>(List.of(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.issuer()),
				new TokenTypeValidator(tokenType)
		));
		validators.addAll(additionalValidators);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
		return decoder;
	}
}
