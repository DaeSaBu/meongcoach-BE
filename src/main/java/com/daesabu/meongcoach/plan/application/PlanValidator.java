package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.exception.PlanExpectationRequiredException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicsRequiredException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlanValidator {

	private static final int EXPECTATION_MIN_LENGTH = 5;
	private static final int EXPECTATION_MAX_LENGTH = 200;
	private static final int TOPICS_MIN_COUNT = 1;
	private static final int TOPICS_MAX_COUNT = 3;

	public void validateRequest(PlanMode mode, PlanCreateRequest request) {
		if (mode == PlanMode.AI) {
			validateExpectation(request.expectation());
			return;
		}
		validateTopics(request.topicIds());
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
