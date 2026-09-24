/**
 * 결제 모듈. RevenueCat이 App Store·Play Store 구매를 통합해 보내는 웹훅을 받아 시기별 이용권(Entitlement)을 부여·회수한다.
 * 회원은 user 모듈의 애그리거트라 회원 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.payment;

import org.springframework.modulith.ApplicationModule;
