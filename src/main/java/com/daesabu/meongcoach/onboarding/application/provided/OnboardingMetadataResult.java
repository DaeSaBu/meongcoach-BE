package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import com.daesabu.meongcoach.training.application.provided.dto.TopicResult;
import java.util.List;

public record OnboardingMetadataResult(List<TopicResult> topics, List<RegisteredBreed> breeds,
                                       List<Personality> personalities) {
}
