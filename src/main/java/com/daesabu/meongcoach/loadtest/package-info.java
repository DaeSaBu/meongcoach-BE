/**
 * JMeter INSERT 부하 측정용 임시 모듈. 인증된 회원이 호출할 때마다 행 하나를 INSERT하는 API만 제공하며, 측정이 끝나면 모듈째 제거한다.
 */
@ApplicationModule
package com.daesabu.meongcoach.loadtest;

import org.springframework.modulith.ApplicationModule;
