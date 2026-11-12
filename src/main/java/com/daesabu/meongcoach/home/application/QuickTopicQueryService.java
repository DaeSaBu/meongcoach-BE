package com.daesabu.meongcoach.home.application;

import com.daesabu.meongcoach.home.application.provided.QuickTopicFinder;
import com.daesabu.meongcoach.home.application.provided.dto.QuickTopicResult;
import com.daesabu.meongcoach.home.domain.QuickTopicRole;
import com.daesabu.meongcoach.plan.application.provided.CurrentPlanFinder;
import com.daesabu.meongcoach.training.application.provided.TopicProgressFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TopicProgressResult;
import com.daesabu.meongcoach.training.domain.shared.ProgressStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
		return findRecommendedTopics(topics);
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

	private List<QuickTopicResult> findRecommendedTopics(List<TopicProgressResult> topics) {
		Stream<QuickTopicResult> inProgress = findMostRecentInProgressTopic(topics).stream()
				.map(topic -> new QuickTopicResult(topic, QuickTopicRole.IN_PROGRESS));
		Stream<QuickTopicResult> notStarted = topics.stream()
				.filter(topic -> topic.status() == ProgressStatus.NOT_STARTED)
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
}
