package com.daesabu.meongcoach.dashboard.application;

import com.daesabu.meongcoach.dashboard.application.provided.DashboardUserFinder;
import com.daesabu.meongcoach.dashboard.domain.exception.DashboardUserNotFoundException;
import com.daesabu.meongcoach.shared.security.AuthorityRole;
import com.daesabu.meongcoach.user.application.provided.RegisteredUserChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUserQueryService implements DashboardUserFinder {

	private final RegisteredUserChecker registeredUserChecker;

	@Override
	public AuthorityRole findRole(Long userId) {
		return registeredUserChecker.findRole(userId)
				.orElseThrow(() -> new DashboardUserNotFoundException(userId));
	}
}
