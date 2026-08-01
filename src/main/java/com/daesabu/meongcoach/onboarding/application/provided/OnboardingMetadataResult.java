package com.daesabu.meongcoach.onboarding.application.provided;

import com.daesabu.meongcoach.dog.application.provided.BreedInfo;
import com.daesabu.meongcoach.dog.application.provided.PersonalityInfo;
import com.daesabu.meongcoach.training.application.provided.TopicSummary;
import java.util.List;

/**
 * 온보딩 화면에 필요한 목록 데이터 묶음.
 */
public record OnboardingMetadataResult(List<TopicSummary> topics, List<BreedInfo> breeds,
                                       List<PersonalityInfo> personalities, List<String> mbtis) {
}
