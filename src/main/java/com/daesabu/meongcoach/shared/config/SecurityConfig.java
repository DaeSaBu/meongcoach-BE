package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.CorsProperties;
import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.shared.security.TokenTypeValidator;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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

	// 로컬 Swagger UI 경로. dev/prod에는 springdoc이 developmentOnly라 존재하지 않아 404가 된다.
	// /v3/api-docs 자체는 열지 않아 springdoc 자동 스캔 스펙은 로컬에서도 노출되지 않는다
	private static final String[] API_DOCS_PATHS = {
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs/swagger-config",
			"/openapi3.json"
	};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
	                                        AuthenticationEntryPoint authenticationEntryPoint,
	                                        AccessDeniedHandler accessDeniedHandler,
	                                        CorsConfigurationSource corsConfigurationSource) {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PERMIT_ALL_PATHS).permitAll()
						.requestMatchers(API_DOCS_PATHS).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(accessTokenDecoder))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.build();
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

	// 액세스·리프레시 디코더를 분리해 각자 용도를 강제한다. @Primary를 두지 않고 주입 지점마다 명시한다
	@Bean
	JwtDecoder accessTokenDecoder(JwtProperties properties) {
		return tokenDecoder(properties, TokenType.ACCESS);
	}

	@Bean
	JwtDecoder refreshTokenDecoder(JwtProperties properties) {
		return tokenDecoder(properties, TokenType.REFRESH);
	}

	private JwtDecoder tokenDecoder(JwtProperties properties, TokenType tokenType) {
		SecretKey secretKey = properties.secretKey();
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(properties.issuer()),
				new TokenTypeValidator(tokenType)
		));
		return decoder;
	}
}
