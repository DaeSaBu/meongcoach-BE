package com.daesabu.meongcoach.dog.domain.shared;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 등록 입력. 다른 모듈이 조립해 provided API에 넘길 수 있도록 노출한다.
 * 견종·성별·성격은 문자열 코드로 받고 enum 변환·검증은 Dog 생성 시점에 일어난다.
 * personalities는 미선택이면 null을 허용하며, profileImageUrl과 expectation은 선택 입력이라 null을 허용한다.
 */
public record DogRegisterCommand(String name, String breed, String sex, LocalDate birthDate,
                                 BigDecimal weightKg, Set<String> personalities, String profileImageUrl,
                                 String expectation) {
}
