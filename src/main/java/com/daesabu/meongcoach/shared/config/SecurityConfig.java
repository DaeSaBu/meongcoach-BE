package com.daesabu.meongcoach.shared.config;

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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

	// 토큰을 아직 받지 못한 요청만 열어둔다. `/api/users/**`로 넓히면 이후 추가될 회원 API까지 공개된다
	private static final String[] PERMIT_ALL_PATHS = {
			"/api/health",
			"/api/users/social/**",
			"/api/users/token/refresh"
	};

	// Swagger UI 정적 파일과 그 안의 openapi3.json이 모두 이 경로 아래에 있다
	private static final String[] API_DOCS_PATHS = {"/swagger-ui/**"};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
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
					auth.anyRequest().authenticated();
				})
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(accessTokenDecoder))
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

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).build();
	}

	// 액세스·리프레시 디코더를 분리해 각자 용도를 강제한다. @Primary를 두지 않고 주입 지점마다 명시한다.
	// 회원 존재 검증기는 user 모듈이 구현한다. shared가 user를 참조하면 순환 의존이 되므로 스프링 타입으로만 받는다
	@Bean
	JwtDecoder accessTokenDecoder(JwtProperties properties, OAuth2TokenValidator<Jwt> registeredUserValidator) {
		return tokenDecoder(properties, TokenType.ACCESS, List.of(registeredUserValidator));
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
