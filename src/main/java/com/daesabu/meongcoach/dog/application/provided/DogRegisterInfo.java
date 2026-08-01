package com.daesabu.meongcoach.dog.application.provided;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 등록 입력. 모듈 경계를 넘는 값이라 enum 대신 문자열을 받고, 변환·검증은 dog 모듈이 수행한다.
 * profileImageUrl은 선택 입력이라 null을 허용한다.
 */
public record DogRegisterInfo(String name, String breed, String sex, LocalDate birthDate,
                              BigDecimal weightKg, Set<String> personalities, String profileImageUrl) {
}
