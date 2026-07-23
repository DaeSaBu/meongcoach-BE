/**
 * 횡단 관심사 모듈. 모든 모듈이 참조할 수 있도록 OPEN 타입으로 공개한다.
 */
@ApplicationModule(type = OPEN)
package com.daesabu.meongcoach.shared;

import static org.springframework.modulith.ApplicationModule.Type.OPEN;

import org.springframework.modulith.ApplicationModule;
