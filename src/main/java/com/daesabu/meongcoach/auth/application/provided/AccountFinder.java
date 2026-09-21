package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.application.provided.dto.EmailAccountFindRequest;
import com.daesabu.meongcoach.auth.domain.EmailAccount;
import com.daesabu.meongcoach.auth.domain.SocialAccount;
import jakarta.validation.Valid;
import java.util.List;

public interface AccountFinder {
	EmailAccount findEmailAccount(@Valid EmailAccountFindRequest emailAccountFindRequest);

	List<SocialAccount> findAllSocialAccount(Long userId);
}
