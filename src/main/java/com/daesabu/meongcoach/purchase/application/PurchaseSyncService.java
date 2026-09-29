package com.daesabu.meongcoach.purchase.application;

import com.daesabu.meongcoach.purchase.application.provided.PurchaseRegister;
import com.daesabu.meongcoach.purchase.application.provided.PurchaseSynchronizer;
import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import com.daesabu.meongcoach.purchase.application.required.StorePurchaseReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseSyncService implements PurchaseSynchronizer {

	private final StorePurchaseReader storePurchaseReader;
	private final PurchaseRegister purchaseRegister;

	/**
	 * 외부 호출을 기다리는 동안 DB 커넥션을 잡지 않도록 트랜잭션 없이 구매 내역을 읽는다.
	 * 저장은 구매 한 건마다 PurchaseRegister의 트랜잭션에 맡기고, 이미 등록한 거래는 그쪽에서 걸러진다.
	 * 저장 중 실패하면 앞서 저장한 구매는 커밋된 채 중단한다. 동시 호출·DB 장애 같은 일시적 실패라 재호출하면 나머지가 등록된다.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void synchronize(Long userId) {
		List<PurchaseRegisterRequest> ownedPurchases = storePurchaseReader.readOwnedPurchases(userId);
		ownedPurchases.forEach(purchaseRegister::register);
	}
}
