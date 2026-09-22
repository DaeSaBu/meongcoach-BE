package com.daesabu.meongcoach.user.application.provided;

import com.daesabu.meongcoach.user.domain.User;

public interface UserFinder {
	boolean isActiveUser(Long userId);

	boolean isOnboardingUser(Long userId);

	User findById(Long userId);
}
