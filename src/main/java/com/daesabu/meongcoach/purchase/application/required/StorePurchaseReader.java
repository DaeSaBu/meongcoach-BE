package com.daesabu.meongcoach.purchase.application.required;

import com.daesabu.meongcoach.purchase.application.provided.dto.PurchaseRegisterRequest;
import java.util.List;

/**
 * 스토어 결제를 통합하는 결제 대행에서 회원의 구매 내역을 읽는다.
 * 환불된 구매는 빼고, 이용권을 유지해야 하는 구매만 등록 입력으로 바꿔 돌려준다.
 */
public interface StorePurchaseReader {

	List<PurchaseRegisterRequest> readOwnedPurchases(Long userId);
}
