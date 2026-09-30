/**
 * 이용권 모듈. 회원이 가진 이용권(Entitlement)을 RevenueCat이 계산한 활성 이용권과 같게 맞춰 우리 DB에 사본으로 둔다.
 * 사본을 두는 것은 이용권이 필요할 때마다 RevenueCat을 호출하지 않아, RevenueCat 장애·호출 한도와 무관하게 읽을 수 있게 하기 위해서다.
 * 상품마다 어떤 이용권을 주는지, 환불되면 무엇을 회수할지는 RevenueCat 대시보드가 원천이라 코드에 복제하지 않는다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.entitlement;

import org.springframework.modulith.ApplicationModule;
