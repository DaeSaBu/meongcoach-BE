package com.daesabu.meongcoach.purchase.adapter.webapi;

import com.daesabu.meongcoach.purchase.application.provided.PurchaseSynchronizer;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

	private final PurchaseSynchronizer purchaseSynchronizer;

	// 앱이 스토어 결제를 마친 직후 호출한다. 웹훅을 기다리지 않고 바로 이용권을 부여받기 위해서다
	@PostMapping("/synchronization")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void synchronize(@CurrentUserId Long userId) {
		purchaseSynchronizer.synchronize(userId);
	}
}
