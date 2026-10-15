package com.daesabu.meongcoach.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.plan.application.provided.dto.PlanCreateRequest;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanMode;
import com.daesabu.meongcoach.plan.domain.exception.PlanExpectationRequiredException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicsRequiredException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanValidatorTest {

	private static final Set<Long> OPEN_IDS = Set.of(11L, 12L, 13L);

	private final PlanValidator validator = new PlanValidator();

	@Test
	void AI는_기대사항이_없으면_거절한다() {
		PlanCreateRequest request = request("AI", null, null);

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_5자_미만이면_거절한다() {
		PlanCreateRequest request = request("AI", null, "  짖음  ");

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_200자를_넘으면_거절한다() {
		PlanCreateRequest request = request("AI", null, "가".repeat(201));

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.AI, request))
				.isInstanceOf(PlanExpectationRequiredException.class);
	}

	@Test
	void AI는_공백을_뺀_기대사항이_5자_이상_200자_이하면_통과한다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, " 짖음 줄이기 ")))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, "가".repeat(5))))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", null, "가".repeat(200))))
				.doesNotThrowAnyException();
	}

	@Test
	void AI는_토픽을_보내도_기대사항만_본다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.AI, request("AI", List.of(1L), "짖음 줄이기")))
				.doesNotThrowAnyException();
	}

	@Test
	void MANUAL은_토픽이_없으면_거절한다() {
		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", null, null)))
				.isInstanceOf(PlanTopicsRequiredException.class);
		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(), null)))
				.isInstanceOf(PlanTopicsRequiredException.class);
	}

	@Test
	void MANUAL은_토픽이_3개를_넘으면_거절한다() {
		PlanCreateRequest request = request("MANUAL", List.of(1L, 2L, 3L, 4L), null);

		assertThatThrownBy(() -> validator.validateRequest(PlanMode.MANUAL, request))
				.isInstanceOf(PlanTopicsRequiredException.class);
	}

	@Test
	void MANUAL은_토픽이_1개_이상_3개_이하면_기대사항_없이_통과한다() {
		assertThatCode(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(1L), null)))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validateRequest(PlanMode.MANUAL, request("MANUAL", List.of(1L, 2L, 3L), null)))
				.doesNotThrowAnyException();
	}

	@Test
	void 올바른_초안은_위반이_없다() {
		PlanDraft draft = draft(week(1, 11L, 12L), week(2, 13L));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).isEmpty();
	}

	@Test
	void 길이가_한도를_넘어도_위반으로_보지_않는다() {
		PlanDraft draft = new PlanDraft("가".repeat(100), "가".repeat(100), "가".repeat(300),
				List.of(week(1, 11L), week(2, 12L)));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).isEmpty();
	}

	@Test
	void 주차가_2개가_아니면_위반이다() {
		assertThat(validator.validateDraft(draft(week(1, 11L)), OPEN_IDS)).containsExactly("weeks는 정확히 2개여야 합니다");
		assertThat(validator.validateDraft(draft(week(1, 11L), week(2, 12L), week(3, 13L)), OPEN_IDS))
				.contains("weeks는 정확히 2개여야 합니다");
	}

	@Test
	void weekNo가_1_2_순서가_아니면_위반이다() {
		PlanDraft draft = draft(week(2, 11L), week(1, 12L));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly("weekNo는 1, 2 순서여야 합니다");
	}

	@Test
	void 주차에_커리큘럼이_없으면_위반이다() {
		PlanDraft draft = draft(week(1, 11L), week(2));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly("2주차에 curriculumIds가 1개 이상 있어야 합니다");
	}

	@Test
	void 열린_카탈로그에_없는_ID는_위반이다() {
		PlanDraft draft = draft(week(1, 11L, 999L), week(2, 12L));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly("카탈로그에 없는 curriculumId입니다: 999");
	}

	@Test
	void 플랜_안에서_중복된_ID는_위반이다() {
		PlanDraft draft = draft(week(1, 11L, 12L), week(2, 12L));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly("중복된 curriculumId입니다: 12");
	}

	@Test
	void 문자열이_공백이면_항목별로_위반이다() {
		PlanDraft draft = new PlanDraft(" ", null, "", List.of(
				new WeekDraft(1, "", "ok", List.of(11L)),
				new WeekDraft(2, "ok", " ", List.of(12L))));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly(
				"title이 비어 있습니다",
				"goal이 비어 있습니다",
				"reason이 비어 있습니다",
				"1주차 theme이 비어 있습니다",
				"2주차 milestone이 비어 있습니다");
	}

	@Test
	void 여러_위반은_모두_모아서_돌려준다() {
		PlanDraft draft = draft(week(1, 999L));

		assertThat(validator.validateDraft(draft, OPEN_IDS)).containsExactly(
				"weeks는 정확히 2개여야 합니다",
				"카탈로그에 없는 curriculumId입니다: 999");
	}

	private static PlanDraft draft(WeekDraft... weeks) {
		return new PlanDraft("제목", "목표", "이유", List.of(weeks));
	}

	private static WeekDraft week(int weekNo, Long... curriculumIds) {
		return new WeekDraft(weekNo, "테마", "마일스톤", List.of(curriculumIds));
	}

	private static PlanCreateRequest request(String mode, List<Long> topicIds, String expectation) {
		return new PlanCreateRequest(2L, mode, topicIds, expectation, 10, 3, false);
	}
}
