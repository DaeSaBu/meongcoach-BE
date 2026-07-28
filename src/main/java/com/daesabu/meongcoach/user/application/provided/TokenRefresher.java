package com.daesabu.meongcoach.user.application.provided;

public interface TokenRefresher {

	AuthToken refresh(String refreshToken);
}
