package com.daesabu.meongcoach.home.application;

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

	private final CurrentPlanFinder currentPlanFinder;
	private final TopicProgressFinder topicProgressFinder;

	@Override
	public List<QuickTopicResult> find(Long userId, Long dogId) {
		List<Long> planTopicIds = currentPlanFinder.findActiveTopicIds(userId, dogId);
		List<TopicProgressResult> topics = topicProgressFinder.findAll(userId).stream()
				.filter(TopicProgressResult::hasThumbnail)
				.toList();
		if (!planTopicIds.isEmpty()) {
			return findPlanTopics(topics, planTopicIds);
		}
		return findRecommendedTopics(topics, userId);
	}

	private List<QuickTopicResult> findPlanTopics(List<TopicProgressResult> topics, List<Long> planTopicIds) {
		Map<Long, TopicProgressResult> topicsById = topics.stream()
				.collect(Collectors.toMap(TopicProgressResult::id, Function.identity()));
		return planTopicIds.stream()
				.map(topicsById::get)
				.filter(Objects::nonNull)
				.limit(LIMIT)
				.map(topic -> new QuickTopicResult(topic, planTopicRole(topic)))
				.toList();
	}

	private QuickTopicRole planTopicRole(TopicProgressResult topic) {
		if (topic.status() == ProgressStatus.IN_PROGRESS) {
			return QuickTopicRole.IN_PROGRESS;
		}
		return QuickTopicRole.NOT_STARTED;
	}

	private List<QuickTopicResult> findRecommendedTopics(List<TopicProgressResult> topics, Long userId) {
		LocalDate today = LocalDate.now(SEOUL);
		long seed = dailySeed(userId, today);
		Stream<QuickTopicResult> inProgress = findMostRecentInProgressTopic(topics).stream()
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS));
		Stream<QuickTopicResult> notStarted = findNotStartedTopics(topics, seed).stream()
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.NOT_STARTED));
		return Stream.concat(inProgress, notStarted)
				.limit(LIMIT)
				.toList();
	}

	private Optional<TopicProgressResult> findMostRecentInProgressTopic(List<TopicProgressResult> topics) {
		Comparator<TopicProgressResult> byLastStudiedAt = Comparator.comparing(TopicProgressResult::lastStudiedAt);
		return topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.IN_PROGRESS)
				.max(byLastStudiedAt);
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
