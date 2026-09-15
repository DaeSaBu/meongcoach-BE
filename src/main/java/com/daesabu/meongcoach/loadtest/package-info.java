/**
 * 부하 테스트 전용 API 모듈. {@code meongcoach.loadtest.enabled}가 true인 환경(local·dev)에서만 활성화되며,
 * 비즈니스 모듈의 provided 인터페이스를 호출해 테스트 데이터를 만든다. 운영에는 컨트롤러 빈이 등록되지 않는다.
 */
@ApplicationModule
package com.daesabu.meongcoach.loadtest;

import org.springframework.modulith.ApplicationModule;
