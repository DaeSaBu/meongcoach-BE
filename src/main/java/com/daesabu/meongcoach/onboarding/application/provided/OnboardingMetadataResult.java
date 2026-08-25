package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.domain.shared.Breed;
import com.daesabu.meongcoach.dog.domain.shared.Personality;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import java.util.List;

/**
 * 온보딩 화면에 필요한 목록 데이터 묶음.
 */
public record OnboardingMetadataResult(List<TopicSummary> topics, List<Breed> breeds,
                                       List<Personality> personalities, List<String> mbtis) {
}
