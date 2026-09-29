/**
 * 구매 모듈. RevenueCat이 App Store·Play Store 구매를 통합해 보내는 웹훅으로 구매(Purchase)를 기록한다.
 * 구매로 얻은 권한은 entitlement 모듈이 가지며, 구매 기록이 멈춰도 이미 부여한 권한 확인은 계속되도록 모듈을 나눴다.
 * 상품마다 어떤 이용권을 주는지는 RevenueCat 대시보드가 원천이라 코드에 복제하지 않고 웹훅이 알려 준 대로 부여한다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.purchase;

import org.springframework.modulith.ApplicationModule;
