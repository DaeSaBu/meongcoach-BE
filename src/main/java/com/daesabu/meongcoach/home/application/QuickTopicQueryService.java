package com.daesabu.meongcoach.home.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileResult;
import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.training.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuickTopicQueryService implements QuickTopicFinder {

	private static final int LIMIT = 5;
	private static final long TWO_YEARS_IN_MONTHS = 24;
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final List<Long> STARTER_UNDER_TWO = List.of(1601L, 1204L, 1303L, 1201L, 1602L);
	private static final List<Long> STARTER_OVER_TWO = List.of(1101L, 1104L, 1102L, 1406L, 1411L);
	private static final Set<Long> EARLY_CATEGORY_IDS = Set.of(16L, 15L);
	private static final Comparator<TopicProgressResult> LATEST_FIRST = Comparator.comparing(
			TopicProgressResult::lastStudiedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

	private final DogProfileFinder dogProfileFinder;
	private final CurrentPlanFinder currentPlanFinder;
	private final TopicProgressFinder topicProgressFinder;

	@Override
	public List<QuickTopicResult> find(Long userId, Long dogId) {
		DogProfileResult dog = dogProfileFinder.findDogProfile(userId, dogId);
		LocalDate today = LocalDate.now(SEOUL);
		boolean overTwo = isOverTwo(dog.birthDate(), today);
		List<TopicProgressResult> topics = topicProgressFinder.findAll(userId);
		if (topics.stream().noneMatch(TopicProgressResult::isStarted)) {
			return findStarters(topics, overTwo);
		}
		List<TopicProgressResult> visible = topics.stream()
				.filter(topic -> !overTwo || !EARLY_CATEGORY_IDS.contains(topic.categoryId()))
				.toList();
		Set<Long> planTopicIds = Set.copyOf(currentPlanFinder.findActiveTopicIds(userId, dogId));
		List<QuickTopicResult> result = new ArrayList<>();
		findContinue(visible, planTopicIds)
				.ifPresent(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.CONTINUE)));
		List<TopicProgressResult> fresh = new ArrayList<>(visible.stream()
				.filter(topic -> !topic.isStarted())
				.filter(topic -> !planTopicIds.contains(topic.id()))
				.toList());
		Collections.shuffle(fresh, new Random(Objects.hash(userId, today)));
		fresh.stream()
				.limit(LIMIT - result.size())
				.forEach(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.NEW)));
		return result;
	}

	private boolean isOverTwo(LocalDate birthDate, LocalDate today) {
		if (birthDate == null) {
			return false;
		}
		return ChronoUnit.MONTHS.between(birthDate, today) >= TWO_YEARS_IN_MONTHS;
	}

	private List<QuickTopicResult> findStarters(List<TopicProgressResult> topics, boolean overTwo) {
		Map<Long, TopicProgressResult> byId = topics.stream()
				.collect(Collectors.toMap(TopicProgressResult::id, Function.identity()));
		List<Long> starterIds = STARTER_UNDER_TWO;
		if (overTwo) {
			starterIds = STARTER_OVER_TWO;
		}
		return starterIds.stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.STARTER))
				.toList();
	}

	private Optional<TopicProgressResult> findContinue(List<TopicProgressResult> topics, Set<Long> planTopicIds) {
		List<TopicProgressResult> inProgress = topics.stream()
				.filter(TopicProgressResult::isInProgress)
				.sorted(LATEST_FIRST)
				.toList();
		return inProgress.stream()
				.filter(topic -> !planTopicIds.contains(topic.id()))
				.findFirst()
				.or(() -> inProgress.stream().findFirst());
	}
}
