/**
 * dog 모듈이 다른 모듈에 노출하는 도메인 타입(견종·성격·성별 enum). 엔티티·일급 컬렉션은 두지 않는다.
 * 다른 모듈은 문자열 코드를 이 enum으로 변환해 provided API에 넘기며, 변환 실패는 dog 도메인 예외로 400이 된다.
 */
@NamedInterface("shared")
package com.daesabu.meongcoach.dog.domain.shared;

import org.springframework.modulith.NamedInterface;
