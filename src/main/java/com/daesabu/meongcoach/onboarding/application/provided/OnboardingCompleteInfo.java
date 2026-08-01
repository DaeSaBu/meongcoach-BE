package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import java.time.LocalDate;
import java.util.List;

/**
 * 온보딩 완료 입력. birthDate, mbti, gender, profileImageUrl은 선택 입력이라 null을 허용한다.
 */
public record OnboardingCompleteInfo(String nickname, LocalDate birthDate, String mbti, String gender,
                                     String profileImageUrl, List<DogRegisterInfo> dogs) {
}
