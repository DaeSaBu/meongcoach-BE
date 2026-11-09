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
import java.time.LocalDateTime;
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
	private static final Comparator<TopicProgressResult> LATEST_FIRST = Comparator.comparing(
			TopicProgressResult::lastStudiedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

	private final DogProfileFinder dogProfileFinder;
	private final CurrentPlanFinder currentPlanFinder;
	private final TopicProgressFinder topicProgressFinder;
	private final QuickTopicProperties properties;

	@Override
	public List<QuickTopicResult> find(Long userId, Long dogId) {
		DogProfileResult dog = dogProfileFinder.findDogProfile(userId, dogId);
		LocalDate today = LocalDate.now(SEOUL);
		boolean older = isOlder(dog.birthDate(), today);
		List<TopicProgressResult> visible = topicProgressFinder.findAll(userId).stream()
				.filter(this::hasThumbnail)
				.filter(topic -> !older || topic.categorySortOrder() >= properties.minCategoryOrder())
				.toList();
		Set<Long> planTopicIds = Set.copyOf(currentPlanFinder.findActiveTopicIds(userId, dogId));
		List<QuickTopicResult> result = new ArrayList<>();
		findContinue(visible, planTopicIds)
				.ifPresent(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS)));
		List<TopicProgressResult> fresh = new ArrayList<>(visible.stream()
				.filter(topic -> topic.status() == ProgressStatus.NOT_STARTED)
				.filter(topic -> !planTopicIds.contains(topic.id()))
				.toList());
		Collections.shuffle(fresh, new Random(Objects.hash(userId, today)));
		fresh.stream()
				.limit(LIMIT - result.size())
				.forEach(topic -> result.add(new QuickTopicResult(topic, QuickTopicRole.NOT_STARTED)));
		return result;
	}

	private boolean hasThumbnail(TopicProgressResult topic) {
		return topic.thumbnailUrl() != null && !topic.thumbnailUrl().isBlank();
	}

	private boolean isOlder(LocalDate birthDate, LocalDate today) {
		if (birthDate == null) {
			return false;
		}
		return ChronoUnit.MONTHS.between(birthDate, today) >= properties.months();
	}

	private Optional<TopicProgressResult> findContinue(List<TopicProgressResult> topics, Set<Long> planTopicIds) {
		List<TopicProgressResult> inProgress = topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.IN_PROGRESS)
				.sorted(LATEST_FIRST)
				.toList();
		return inProgress.stream()
				.filter(topic -> !planTopicIds.contains(topic.id()))
				.findFirst()
				.or(() -> inProgress.stream().findFirst());
	}
}
