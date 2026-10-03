package com.daesabu.meongcoach.shared.config;

import com.daesabu.meongcoach.shared.security.AppVersionFilter;
import com.daesabu.meongcoach.shared.security.AppVersionProperties;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.shared.security.JwtProperties;
import com.daesabu.meongcoach.shared.security.TokenType;
import com.daesabu.meongcoach.shared.security.TokenTypeValidator;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PERMIT_ALL_PATHS = {
			"/api/health",
			"/api/auth/login/social",
			"/api/auth/login/email",
			"/api/auth/token/refresh",
			"/api/auth/logout"
	};

	private static final String[] ONBOARDING_ALLOWED_PATHS = {
			"/api/onboarding/**",
			"/api/dogs/profile/image"
	};

	private static final String WITHDRAW_PATH = "/api/auth/me";

	private static final String MY_INFO_PATH = "/api/users/me";

	private static final String[] API_DOCS_PATHS = {"/swagger-ui/**"};

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder accessTokenDecoder,
	                                        Converter<Jwt, AbstractAuthenticationToken> userRoleAuthenticationConverter,
	                                        AuthenticationEntryPoint authenticationEntryPoint,
	                                        AccessDeniedHandler accessDeniedHandler,
	                                        AppVersionProperties appVersionProperties,
	                                        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
	                                        @Value("${meongcoach.api-docs.enabled:false}") boolean apiDocsEnabled) {
		return http
				.addFilterBefore(new AppVersionFilter(appVersionProperties, resolver), DisableEncodeUrlFilter.class)
				.cors(AbstractHttpConfigurer::disable)
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
					auth.requestMatchers(ONBOARDING_ALLOWED_PATHS)
							.hasAnyRole(AuthorityRole.USER.name(), AuthorityRole.ONBOARDING_USER.name());
					auth.requestMatchers(HttpMethod.DELETE, WITHDRAW_PATH)
							.hasAnyRole(AuthorityRole.USER.name(), AuthorityRole.ONBOARDING_USER.name());
					auth.requestMatchers(HttpMethod.GET, MY_INFO_PATH)
							.hasAnyRole(AuthorityRole.USER.name(), AuthorityRole.ONBOARDING_USER.name());
					auth.anyRequest().hasRole(AuthorityRole.USER.name());
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

	private void configureApiDocsAccess(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
			boolean apiDocsEnabled) {
		if (apiDocsEnabled) {
			auth.requestMatchers(API_DOCS_PATHS).permitAll();
			return;
		}
		auth.requestMatchers(API_DOCS_PATHS).denyAll();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey()).build();
	}

	@Bean
	JwtDecoder accessTokenDecoder(JwtProperties properties) {
		return tokenDecoder(properties, TokenType.ACCESS, List.of());
	}

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
