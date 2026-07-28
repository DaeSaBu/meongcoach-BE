package com.daesabu.meongcoach.user.application;

import com.daesabu.meongcoach.user.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.SocialAccount;
import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserStatus;
import com.daesabu.meongcoach.user.domain.exception.WithdrawnUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 계정에 연결된 회원을 찾거나 등록한다.
 * 로그인 흐름에서 원자성이 필요한 DB 작업만 맡아, 외부 제공자 호출이 트랜잭션에 묶이지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SocialUserRegisterService {

	private final UserRepository userRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final UserProfileRepository userProfileRepository;

	/**
	 * 지연 로딩 프록시가 트랜잭션 밖으로 새지 않도록 엔티티 대신 필요한 값만 반환한다.
	 */
	public LoginUser findOrRegister(SocialAccountLinkCommand command) {
		User user = findOrRegisterUser(command);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new WithdrawnUserException();
		}
		return new LoginUser(user.getId(), !userProfileRepository.existsById(user.getId()));
	}

	// 회원 생성과 소셜 계정 연동은 같은 트랜잭션에서 일어나야 한다
	private User findOrRegisterUser(SocialAccountLinkCommand command) {
		return socialAccountRepository.findByProviderAndProviderId(command.provider(), command.providerId())
				.map(SocialAccount::getUser)
				.orElseGet(() -> register(command));
	}

	private User register(SocialAccountLinkCommand command) {
		User user = userRepository.save(User.registerMember());
		socialAccountRepository.save(SocialAccount.link(user, command));
		return user;
	}

	public record LoginUser(Long userId, boolean needsOnboarding) {
	}
}
