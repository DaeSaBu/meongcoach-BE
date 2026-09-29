package com.daesabu.meongcoach.entitlement.application.required;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Set;

/**
 * 스토어 결제를 통합하는 결제 대행에서 회원이 지금 쓸 수 있는 이용권 종류를 읽는다. 환불·계정 이전 반영은 결제 대행이 끝낸 결과다.
 */
public interface ActiveEntitlementReader {

	Set<EntitlementType> readActiveTypes(Long userId);
}
