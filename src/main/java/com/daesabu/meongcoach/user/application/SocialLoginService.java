package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.provided.AuthToken;
import com.daesabu.meongcoach.user.application.provided.SocialLogin;
import com.daesabu.meongcoach.user.application.provided.SocialLoginResult;
import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.application.required.TokenProvider;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.UnsupportedSocialProviderException;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 자격증명을 검증하고 회원을 조회·생성한 뒤 우리 서비스 토큰을 발급한다.
 * 제공자 구현체는 스프링이 주입하므로 제공자를 늘려도 이 클래스는 바뀌지 않는다.
 */
@Service
@Transactional
public class SocialLoginService implements SocialLogin {

	private final Map<SocialProvider, SocialProfileReader> readers;
	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final TokenProvider tokenProvider;

	public SocialLoginService(List<SocialProfileReader> readers, UserRepository userRepository,
	                          SocialAccountRepository socialAccountRepository,
	                          UserProfileRepository userProfileRepository, TokenProvider tokenProvider) {
		this.readers = readers.stream()
				.collect(Collectors.toUnmodifiableMap(SocialProfileReader::provider, Function.identity()));
		this.userRepository = userRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.userProfileRepository = userProfileRepository;
		this.tokenProvider = tokenProvider;
	}

	@Override
	public SocialLoginResult login(SocialProvider provider, String credential) {
		SocialAccountLinkCommand command = reader(provider).read(credential);
		User user = findOrRegister(command);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new WithdrawnUserException();
		}

		AuthToken token = tokenProvider.issue(user.getId());
		boolean needsOnboarding = !userProfileRepository.existsById(user.getId());

		return new SocialLoginResult(token, needsOnboarding);
	}

	private SocialProfileReader reader(SocialProvider provider) {
		SocialProfileReader reader = readers.get(provider);
		if (reader == null) {
			throw new UnsupportedSocialProviderException(provider.name());
		}
		return reader;
	}

	// 회원 생성과 소셜 계정 연동은 같은 트랜잭션에서 일어나야 한다
	private User findOrRegister(SocialAccountLinkCommand command) {
		return socialAccountRepository.findByProviderAndProviderId(command.provider(), command.providerId())
				.map(SocialAccount::getUser)
				.orElseGet(() -> register(command));
	}

	private User register(SocialAccountLinkCommand command) {
		User user = userRepository.save(User.registerMember());
		socialAccountRepository.save(SocialAccount.link(user, command));
		return user;
	}
}
