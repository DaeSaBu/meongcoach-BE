package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import java.util.List;

public record OnboardingMetadataResult(List<RegisteredBreed> breeds,
                                       List<Personality> personalities) {
}
