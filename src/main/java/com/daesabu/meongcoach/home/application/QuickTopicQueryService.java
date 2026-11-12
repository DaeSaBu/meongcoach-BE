package com.daesabu.meongcoach.home.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.training.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

	@Override
	public List<QuickTopicResult> find(Long userId, Long dogId) {
		dogProfileFinder.findDogProfile(userId, dogId);
		LocalDate today = LocalDate.now(SEOUL);
		List<TopicProgressResult> topics = topicProgressFinder.findAll(userId).stream()
				.filter(TopicProgressResult::hasThumbnail)
				.toList();
		List<Long> planTopicIds = currentPlanFinder.findActiveTopicIds(userId, dogId);
		if (!planTopicIds.isEmpty()) {
			List<QuickTopicResult> planTopics = findPlanTopics(topics, planTopicIds);
			return planTopics.stream().limit(LIMIT).toList();
		}

		Optional<QuickTopicResult> inProgressTopic = findInProgressTopic(topics)
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS));
		List<QuickTopicResult> notStartedTopics = findNotStartedTopics(topics, dailySeed(userId, today))
				.stream()
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.NOT_STARTED))
				.toList();

		Stream<QuickTopicResult> results = Stream.concat(inProgressTopic.stream(), notStartedTopics.stream());
		return results
				.limit(LIMIT)
				.toList();
	}

	private List<QuickTopicResult> findPlanTopics(List<TopicProgressResult> topics, List<Long> planTopicIds) {
		Map<Long, TopicProgressResult> topicsById = topics.stream()
				.collect(Collectors.toMap(TopicProgressResult::id, Function.identity()));
		return planTopicIds.stream()
				.map(topicsById::get)
				.filter(Objects::nonNull)
				.map(topic -> new QuickTopicResult(topic, planTopicRole(topic)))
				.toList();
	}

	private QuickTopicRole planTopicRole(TopicProgressResult topic) {
		if (topic.status() == ProgressStatus.IN_PROGRESS) {
			return QuickTopicRole.IN_PROGRESS;
		}
		return QuickTopicRole.NOT_STARTED;
	}

	private Optional<TopicProgressResult> findInProgressTopic(List<TopicProgressResult> topics) {
		Comparator<TopicProgressResult> latestFirst = Comparator.comparing(TopicProgressResult::lastStudiedAt).reversed();
		return topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.IN_PROGRESS)
				.min(latestFirst);
	}

	private List<TopicProgressResult> findNotStartedTopics(List<TopicProgressResult> topics, long seed) {
		List<TopicProgressResult> notStarted = new ArrayList<>(topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.NOT_STARTED)
				.toList());
		Collections.shuffle(notStarted, new Random(seed));
		return notStarted;
	}

	private long dailySeed(Long userId, LocalDate today) {
		return Objects.hash(userId, today);
	}
}
