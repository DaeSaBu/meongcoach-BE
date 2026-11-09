package com.daesabu.meongcoach.home.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.training.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuickTopicQueryService implements QuickTopicFinder {

	private static final int LIMIT = 5;
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final DogProfileFinder dogProfileFinder;
	private final CurrentPlanFinder currentPlanFinder;
	private final TopicProgressFinder topicProgressFinder;
	private final QuickTopicProperties properties;

	@Override
	public List<QuickTopicResult> find(Long userId, Long dogId) {
		DogProfileResult dog = dogProfileFinder.findDogProfile(userId, dogId);
		LocalDate today = LocalDate.now(SEOUL);
		int minCategoryOrder = minCategoryOrder(dog.birthDate(), today);
		List<TopicProgressResult> candidates = topicProgressFinder.findAll(userId).stream()
				.filter(TopicProgressResult::hasThumbnail)
				.filter(topic -> topic.categorySortOrder() >= minCategoryOrder)
				.toList();
		Set<Long> planTopicIds = Set.copyOf(currentPlanFinder.findActiveTopicIds(userId, dogId));

		List<QuickTopicResult> result = new ArrayList<>();
		findInProgressTopic(candidates, planTopicIds)
				.ifPresent(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS)));
		findNotStartedTopics(candidates, planTopicIds, dailySeed(userId, today)).stream()
				.limit(LIMIT - result.size())
				.forEach(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.NOT_STARTED)));
		return result;
	}

	private int minCategoryOrder(LocalDate birthDate, LocalDate today) {
		if (isOverMonths(birthDate, today)) {
			return properties.minCategoryOrder();
		}
		return 0;
	}

	private boolean isOverMonths(LocalDate birthDate, LocalDate today) {
		if (birthDate == null) {
			return false;
		}
		return ChronoUnit.MONTHS.between(birthDate, today) >= properties.months();
	}

	private Optional<TopicProgressResult> findInProgressTopic(List<TopicProgressResult> topics, Set<Long> planTopicIds) {
		Comparator<TopicProgressResult> outsidePlanFirst = Comparator.comparing(topic -> planTopicIds.contains(topic.id()));
		Comparator<TopicProgressResult> latestFirst = Comparator.comparing(TopicProgressResult::lastStudiedAt).reversed();
		return topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.IN_PROGRESS)
				.min(outsidePlanFirst.thenComparing(latestFirst));
	}

	private List<TopicProgressResult> findNotStartedTopics(List<TopicProgressResult> topics, Set<Long> planTopicIds,
			long seed) {
		List<TopicProgressResult> notStarted = new ArrayList<>(topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.NOT_STARTED)
				.filter(topic -> !planTopicIds.contains(topic.id()))
				.toList());
		Collections.shuffle(notStarted, new Random(seed));
		return notStarted;
	}

	private long dailySeed(Long userId, LocalDate today) {
		return Objects.hash(userId, today);
	}
}
