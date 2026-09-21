package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.auth.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
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

	private final UserRegister userRegister;
	private final SocialAccountRepository socialAccountRepository;

	public Long findOrRegister(SocialAccountLinkCommand command) {
		return socialAccountRepository.findByProviderAndProviderId(command.provider(), command.providerId())
				.map(SocialAccount::getUserId)
				.orElseGet(() -> register(command));
	}

	// 회원 생성과 소셜 계정 연동은 같은 트랜잭션에서 일어나야 한다
	private Long register(SocialAccountLinkCommand command) {
		Long userId = userRegister.register();
		socialAccountRepository.save(SocialAccount.link(userId, command));
		return userId;
	}
}
