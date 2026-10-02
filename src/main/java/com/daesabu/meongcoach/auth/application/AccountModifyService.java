package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AccountRegister;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialAccountRegisterRequest;
import com.daesabu.meongcoach.auth.application.required.EmailAccountRepository;
import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.user.application.provided.UserRegister;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountModifyService implements AccountRegister {
	private final SocialAccountRepository socialAccountRepository;
	private final UserRegister userRegister;
	private final EmailAccountRepository emailAccountRepository;

	@Override
	@Transactional
	public Long upsertSocialAccount(SocialAccountRegisterRequest socialAccountRegisterRequest) {
		return socialAccountRepository.findByProviderAndProviderId(
						socialAccountRegisterRequest.provider(),
						socialAccountRegisterRequest.providerId())
				.map(SocialAccount::getUserId)
				.orElseGet(() -> registerUser(socialAccountRegisterRequest));
	}

	@Override
	@Transactional
	public void deleteAllSocialAccounts(Long userId) {
		socialAccountRepository.deleteAllByUserId(userId);
	}

	@Override
	@Transactional
	public void deleteAllEmailAccounts(Long userId) {
		emailAccountRepository.deleteAllByUserId(userId);
	}

	private Long registerUser(SocialAccountRegisterRequest socialAccountRegisterRequest) {
		Long userId = userRegister.register();

		SocialAccount socialAccount = SocialAccount.register(userId, socialAccountRegisterRequest.toCommand());

		socialAccountRepository.save(socialAccount);

		return userId;
	}
}
