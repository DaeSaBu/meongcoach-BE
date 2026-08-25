package com.daesabu.meongcoach.dog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 프로필 전체 교체 입력. 견종·성별·성격은 문자열 코드로 받고 enum 변환·검증은 Dog.updateProfile에서 일어난다.
 * personalities는 미선택이면 null을 허용한다.
 */
public record DogProfileUpdateCommand(String name, String breed, String sex, LocalDate birthDate,
                                      BigDecimal weightKg, Set<String> personalities,
                                      String profileImageUrl, String expectation) {
}
