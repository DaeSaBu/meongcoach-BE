package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.application.provided.dto.EmailLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.LogoutRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.SocialLoginRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.TokenRefreshRequest;
import com.daesabu.meongcoach.auth.application.provided.dto.WithdrawRequest;
import com.daesabu.meongcoach.auth.domain.AuthToken;
import jakarta.validation.Valid;

public interface Authenticator {
	AuthToken socialLogin(@Valid SocialLoginRequest socialLoginRequest);

	AuthToken emailLogin(@Valid EmailLoginRequest emailLoginRequest);

	void logout(@Valid LogoutRequest logoutRequest);

	AuthToken refresh(@Valid TokenRefreshRequest tokenRefreshRequest);

	void withdraw(Long userId, WithdrawRequest withdrawRequest);
}
