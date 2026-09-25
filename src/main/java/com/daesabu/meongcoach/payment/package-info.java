/**
 * 결제 모듈. RevenueCat이 App Store·Play Store 구매를 통합해 보내는 웹훅으로 구매(Purchase)를 기록한다.
 * 구매로 얻은 권한은 entitlement 모듈이 가지며, 결제가 멈춰도 이미 부여한 권한 확인은 계속되도록 모듈을 나눴다.
 * 상품과 상품별 권한 구성은 RevenueCat 대시보드가 원천이라 코드에 복제하지 않는다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.payment;

import org.springframework.modulith.ApplicationModule;
