package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.shared.security.TokenTypeValidator;
import javax.crypto.SecretKey;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
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

/**
 * 인증·인가 구성. 클라이언트가 네이티브 앱뿐이라 세션·CSRF·폼 로그인이 필요 없고,
 * 자체 발급 JWT를 Bearer 토큰으로 검증하는 무상태 필터 체인을 둔다.
 * h2-console 전용 체인은 local 프로파일에서만 추가로 등록된다.
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

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
	                                        AuthenticationEntryPoint authenticationEntryPoint,
	                                        AccessDeniedHandler accessDeniedHandler) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.rememberMe(AbstractHttpConfigurer::disable)
				.anonymous(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PERMIT_ALL_PATHS).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.decoder(accessTokenDecoder))
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.build();
	}

	// h2-console은 local 전용 별도 체인으로 분리한다. local 외 프로파일에서는 이 빈이 없어
	// 콘솔 경로도 메인 체인의 anyRequest().authenticated()에 걸린다
	@Bean
	@Profile("local")
	@Order(0)
	SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher(PathRequest.toH2Console())
				.csrf(AbstractHttpConfigurer::disable)
				// h2-console은 프레임을 쓰므로 동일 출처 프레임만 허용한다
				.headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.build();
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
