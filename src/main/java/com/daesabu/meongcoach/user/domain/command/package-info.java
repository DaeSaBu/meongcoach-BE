/**
 * user 모듈의 생성 커맨드. 다른 모듈이 커맨드를 조립해 provided API를 호출할 수 있도록 노출한다.
 * 문자열 코드의 변환·검증은 커맨드를 소비하는 도메인 객체가 수행한다.
 */
@NamedInterface("command")
package com.daesabu.meongcoach.user.domain.command;

import org.springframework.modulith.NamedInterface;
