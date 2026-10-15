package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.exception.PlanExpectationRequiredException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicsRequiredException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class PlanValidator {

	private static final int EXPECTATION_MIN_LENGTH = 5;
	private static final int EXPECTATION_MAX_LENGTH = 200;
	private static final int TOPICS_MIN_COUNT = 1;
	private static final int TOPICS_MAX_COUNT = 3;
	private static final int DRAFT_WEEKS = 2;

	public void validateRequest(PlanMode mode, PlanCreateRequest request) {
		if (mode == PlanMode.AI) {
			validateExpectation(request.expectation());
			return;
		}
		validateTopics(request.topicIds());
	}

	public List<String> validateDraft(PlanDraft draft, Set<Long> openCurriculumIds) {
		List<String> violations = new ArrayList<>();
		List<WeekDraft> weeks = draft.weeks();
		if (weeks.size() != DRAFT_WEEKS) {
			violations.add("weeks는 정확히 2개여야 합니다");
		}
		if (!hasExpectedWeekNos(weeks)) {
			violations.add("weekNo는 1, 2 순서여야 합니다");
		}
		weeks.stream()
				.filter(week -> week.curriculumIds().isEmpty())
				.forEach(week -> violations.add(week.weekNo() + "주차에 curriculumIds가 1개 이상 있어야 합니다"));
		List<Long> allIds = weeks.stream()
				.flatMap(week -> week.curriculumIds().stream())
				.toList();
		allIds.stream()
				.filter(id -> id == null || !openCurriculumIds.contains(id))
				.distinct()
				.forEach(id -> violations.add("카탈로그에 없는 curriculumId입니다: " + id));
		duplicates(allIds).forEach(id -> violations.add("중복된 curriculumId입니다: " + id));
		addBlankViolations(draft, violations);
		return violations;
	}

	private boolean hasExpectedWeekNos(List<WeekDraft> weeks) {
		return IntStream.range(0, weeks.size())
				.allMatch(index -> weeks.get(index).weekNo() == index + 1);
	}

	private List<Long> duplicates(List<Long> ids) {
		Set<Long> seen = new HashSet<>();
		return ids.stream()
				.filter(Objects::nonNull)
				.filter(id -> !seen.add(id))
				.distinct()
				.collect(Collectors.toList());
	}

	private void addBlankViolations(PlanDraft draft, List<String> violations) {
		addIfBlank(draft.title(), "title", violations);
		addIfBlank(draft.goal(), "goal", violations);
		addIfBlank(draft.reason(), "reason", violations);
		for (WeekDraft week : draft.weeks()) {
			addIfBlank(week.theme(), week.weekNo() + "주차 theme", violations);
			addIfBlank(week.milestone(), week.weekNo() + "주차 milestone", violations);
		}
	}

	private void addIfBlank(String value, String name, List<String> violations) {
		if (value == null || value.isBlank()) {
			violations.add(name + "이 비어 있습니다");
		}
	}

	private void validateExpectation(String expectation) {
		if (expectation == null) {
			throw new PlanExpectationRequiredException();
		}
		int length = expectation.strip().length();
		if (length < EXPECTATION_MIN_LENGTH || length > EXPECTATION_MAX_LENGTH) {
			throw new PlanExpectationRequiredException();
		}
	}

	private void validateTopics(List<Long> topicIds) {
		if (topicIds == null) {
			throw new PlanTopicsRequiredException();
		}
		if (topicIds.size() < TOPICS_MIN_COUNT || topicIds.size() > TOPICS_MAX_COUNT) {
			throw new PlanTopicsRequiredException();
		}
	}
}
