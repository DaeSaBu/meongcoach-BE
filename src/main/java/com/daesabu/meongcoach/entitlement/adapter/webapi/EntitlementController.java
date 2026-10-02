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

	@PostMapping("/synchronization")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void synchronize(@CurrentUserId Long userId) {
		entitlementSynchronizer.synchronize(userId);
	}
}
