package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AuthToken;
import com.daesabu.meongcoach.auth.application.provided.LoginResult;
import com.daesabu.meongcoach.auth.application.provided.SocialLogin;
import com.daesabu.meongcoach.auth.application.required.SocialProfileReader;
import com.daesabu.meongcoach.auth.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.auth.domain.SocialProvider;
import com.daesabu.meongcoach.auth.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.auth.domain.exception.WithdrawnUserException;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 소셜 자격증명을 검증하고 회원을 조회·생성한 뒤 우리 서비스 토큰을 발급한다.
 * 제공자 구현체는 스프링이 주입하므로 제공자를 늘려도 이 클래스는 바뀌지 않는다.
 * 제공자 호출은 롤백할 것이 없고 커넥션을 잡을 이유도 없으므로 이 클래스는 트랜잭션을 열지 않고,
 * 회원 조회·등록(SocialUserRegisterService)과 토큰 발급·저장(AuthTokenIssueService)이 각자 트랜잭션을 연다.
 */
@Service
public class SocialLoginService implements SocialLogin {

	private final Map<SocialProvider, SocialProfileReader> readers;
	private final SocialUserRegisterService socialUserRegisterService;
	private final AuthTokenIssueService authTokenIssueService;
	private final RegisteredUserChecker registeredUserChecker;

	public SocialLoginService(List<SocialProfileReader> readers,
	                          SocialUserRegisterService socialUserRegisterService,
	                          AuthTokenIssueService authTokenIssueService,
	                          RegisteredUserChecker registeredUserChecker) {
		this.readers = readers.stream()
				.collect(Collectors.toUnmodifiableMap(SocialProfileReader::provider, Function.identity()));
		this.socialUserRegisterService = socialUserRegisterService;
		this.authTokenIssueService = authTokenIssueService;
		this.registeredUserChecker = registeredUserChecker;
	}

	@Override
	public LoginResult login(SocialProvider provider, String credential) {
		SocialAccountLinkCommand command = getSocialAccountLinkCommand(provider, credential);

		Long userId = socialUserRegisterService.findOrRegister(command);

		// 등록된 회원만 role이 조회된다. 자격증명이 남아 있는데 조회되지 않으면 탈퇴한 회원이다
		AuthorityRole role = registeredUserChecker.findRole(userId)
				.orElseThrow(WithdrawnUserException::new);

		AuthToken token = authTokenIssueService.issue(userId);

		boolean needsOnboarding = role == AuthorityRole.ONBOARDING_USER;

		return new LoginResult(token, needsOnboarding);
	}

	private SocialAccountLinkCommand getSocialAccountLinkCommand(SocialProvider provider, String credential){
		SocialProfileReader reader = readers.get(provider);
		if (reader == null) {
			throw new UnsupportedSocialProviderException(provider.name());
		}
		return reader.read(credential);
	}
}
