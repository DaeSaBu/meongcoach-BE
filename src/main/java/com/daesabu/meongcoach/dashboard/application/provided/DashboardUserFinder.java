package com.daesabu.meongcoach.dashboard.application.provided;

import com.daesabu.meongcoach.shared.security.AuthorityRole;

public interface DashboardUserFinder {

	/**
	 * 회원의 역할을 반환한다. 없거나 탈퇴한 회원이면 DashboardUserNotFoundException을 던진다.
	 */
	AuthorityRole findRole(Long userId);
}
