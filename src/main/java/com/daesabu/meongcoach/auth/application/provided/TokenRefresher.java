package com.daesabu.meongcoach.auth.application.provided;

public interface TokenRefresher {

	AuthToken refresh(String refreshToken);
}
