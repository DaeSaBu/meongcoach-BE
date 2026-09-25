/**
 * 결제 모듈. RevenueCat이 App Store·Play Store 구매를 통합해 보내는 웹훅으로 구매(Purchase)를 기록하고, 웹훅이 알려 준 권한(Entitlement)을 부여·회수한다.
 * 상품과 상품별 권한 구성은 RevenueCat 대시보드가 원천이라 코드에 복제하지 않는다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.payment;

import org.springframework.modulith.ApplicationModule;
