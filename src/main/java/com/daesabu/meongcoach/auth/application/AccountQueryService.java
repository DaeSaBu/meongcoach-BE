package com.daesabu.meongcoach.auth.application;

import com.daesabu.meongcoach.auth.application.provided.AccountFinder;
import com.daesabu.meongcoach.auth.application.provided.dto.EmailAccountFindRequest;
import com.daesabu.meongcoach.auth.application.required.EmailAccountRepository;
import com.daesabu.meongcoach.auth.application.required.SocialAccountRepository;
import com.daesabu.meongcoach.auth.domain.EmailAccount;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import com.daesabu.meongcoach.auth.domain.exception.InvalidEmailException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountQueryService implements AccountFinder {
	private final EmailAccountRepository emailAccountRepository;
	private final SocialAccountRepository socialAccountRepository;

	@Override
	public EmailAccount findEmailAccount(EmailAccountFindRequest emailAccountFindRequest) {
		return emailAccountRepository.findByEmail(emailAccountFindRequest.email())
				.orElseThrow(InvalidEmailException::new);
	}

	@Override
	public List<SocialAccount> findAllSocialAccount(Long userId) {
		return socialAccountRepository.findAllByUserId(userId);
	}
}
