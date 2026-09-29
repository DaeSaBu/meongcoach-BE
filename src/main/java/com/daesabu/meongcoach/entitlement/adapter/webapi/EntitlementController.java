package com.daesabu.meongcoach.entitlement.adapter.webapi;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementSynchronizer;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
public class EntitlementController {

	private final EntitlementSynchronizer entitlementSynchronizer;

	// 앱이 스토어 결제·복원을 마친 직후 호출한다. 웹훅은 재시도 간격 때문에 늦게 올 수 있어 결제 직후 부여는 이 호출이 맡는다
	@PostMapping("/synchronization")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void synchronize(@CurrentUserId Long userId) {
		entitlementSynchronizer.synchronize(userId);
	}
}
