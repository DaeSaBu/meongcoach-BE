/**
 * 이용권 모듈. 회원이 가진 이용권(Entitlement)을 RevenueCat이 계산한 활성 이용권과 같게 맞춰 두고, 다른 모듈이 기능을 열어 줄지 확인할 수 있게 한다.
 * 권한 확인은 유료 기능마다 호출되므로 RevenueCat 대신 우리 DB의 사본만 읽어, RevenueCat 장애·호출 한도와 무관하게 동작한다.
 * 상품마다 어떤 이용권을 주는지, 환불되면 무엇을 회수할지는 RevenueCat 대시보드가 원천이라 코드에 복제하지 않는다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.entitlement;

import org.springframework.modulith.ApplicationModule;
