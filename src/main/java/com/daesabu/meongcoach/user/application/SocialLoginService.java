package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.command.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 소셜 자격증명을 검증하고 회원을 조회·생성한 뒤 우리 서비스 토큰을 발급한다.
 * 제공자 구현체는 스프링이 주입하므로 제공자를 늘려도 이 클래스는 바뀌지 않는다.
 * 제공자 호출과 토큰 발급은 롤백할 것이 없고 커넥션을 잡을 이유도 없으므로,
 * 트랜잭션은 회원 조회·등록을 맡는 SocialUserRegisterService 안에서만 열린다.
 */
@Service
public class SocialLoginService implements SocialLogin {

	private final Map<SocialProvider, SocialProfileReader> readers;
	private final SocialUserRegisterService socialUserRegisterService;
	private final TokenProvider tokenProvider;

	public SocialLoginService(List<SocialProfileReader> readers,
	                          SocialUserRegisterService socialUserRegisterService, TokenProvider tokenProvider) {
		this.readers = readers.stream()
				.collect(Collectors.toUnmodifiableMap(SocialProfileReader::provider, Function.identity()));
		this.socialUserRegisterService = socialUserRegisterService;
		this.tokenProvider = tokenProvider;
	}

	@Override
	public SocialLoginResult login(SocialProvider provider, String credential) {
		SocialAccountLinkCommand command = getSocialAccountLinkCommand(provider, credential);
		User user = socialUserRegisterService.findOrRegister(command);

		AuthToken token = tokenProvider.issue(user.getId());
		boolean needsOnboarding = socialUserRegisterService.needsOnboarding(user.getId());

		return new SocialLoginResult(token, needsOnboarding);
	}

	private SocialAccountLinkCommand getSocialAccountLinkCommand(SocialProvider provider, String credential){
		SocialProfileReader reader = readers.get(provider);
		if (reader == null) {
			throw new UnsupportedSocialProviderException(provider.name());
		}
		return reader.read(credential);
	}
}
