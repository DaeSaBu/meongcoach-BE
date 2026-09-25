/**
 * 이용권 모듈. 회원이 가진 권한(Entitlement)을 부여·회수하고, 다른 모듈이 기능을 열어 줄지 확인할 수 있게 한다.
 * 권한 확인은 유료 기능마다 호출되므로 결제 모듈과 분리해 구매 기록이 멈춰도 계속 동작하게 한다.
 * 권한을 준 구매는 payment 모듈의, 회원은 user 모듈의 애그리거트라 ID로만 참조한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.entitlement;

import org.springframework.modulith.ApplicationModule;
