package com.daesabu.meongcoach.auth.application.provided;

import com.daesabu.meongcoach.auth.domain.AuthToken;

public interface AuthTokenProvider {
	AuthToken issue(Long userId);
}
