package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.application.provided.DogRegisterInfo;
import java.util.List;

public record OnboardingCompleteInfo(String nickname, List<DogRegisterInfo> dogs) {
}
