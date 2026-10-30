package com.daesabu.meongcoach.dashboard.domain.exception;

import com.daesabu.meongcoach.shared.exception.DomainException;

public class DashboardUserNotFoundException extends DomainException {

	public DashboardUserNotFoundException(Long userId) {
		super(DashboardErrorCode.DASHBOARD_USER_NOT_FOUND, "id가 " + userId + "인 회원을 찾을 수 없습니다.");
	}
}
