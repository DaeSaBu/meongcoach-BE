package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.application.provided.dto.SocialAccountRegisterRequest;
import jakarta.validation.Valid;

public interface AccountRegister {
	Long upsertSocialAccount(@Valid SocialAccountRegisterRequest socialAccountRegisterRequest);

	void deleteAllSocialAccounts(Long userId);

	void deleteAllEmailAccounts(Long userId);
}
