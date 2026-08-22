package com.daesabu.meongcoach.dog.application.provided;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 강아지 프로필 수정 입력. 등록 입력과 같이 enum 대신 문자열을 받고, 변환·검증은 dog 모듈이 수행한다.
 * 전체 교체 입력이라 birthDate·personalities·profileImageUrl·expectation의 null은 '값 없음'으로 저장된다.
 */
public record DogProfileUpdateInfo(String name, String breed, String sex, LocalDate birthDate,
                                   BigDecimal weightKg, Set<String> personalities, String profileImageUrl,
                                   String expectation) {
}
