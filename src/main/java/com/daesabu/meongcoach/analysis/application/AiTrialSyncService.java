package com.daesabu.meongcoach.analysis.application;

import com.daesabu.meongcoach.analysis.application.provided.AiTrialSynchronizer;
import com.daesabu.meongcoach.analysis.application.required.PurchasedTrialCountReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTrialSyncService implements AiTrialSynchronizer {

	private final PurchasedTrialCountReader purchasedTrialCountReader;
	private final AiTrialModifyService aiTrialModifyService;

	@Override
	public void synchronize(Long userId) {
		int purchasedCount = purchasedTrialCountReader.readPurchasedCount(userId);

		aiTrialModifyService.synchronizePurchasedCount(userId, purchasedCount);
	}
}
