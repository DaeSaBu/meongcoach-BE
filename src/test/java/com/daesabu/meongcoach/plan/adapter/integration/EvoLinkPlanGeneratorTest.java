package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.daesabu.meongcoach.ai.application.provided.StructuredChatCompleter;
import com.daesabu.meongcoach.ai.application.provided.dto.StructuredChatRequest;
import com.daesabu.meongcoach.ai.domain.exception.StructuredChatCompletionFailedException;
import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.PlanGenerationContext;
import com.daesabu.meongcoach.plan.domain.PlanDraft;
import com.daesabu.meongcoach.plan.domain.exception.PlanGenerationFailedException;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class EvoLinkPlanGeneratorTest {

	private static final String VALID_JSON = """
			{"title":"초코의 2주 앉아 플랜","goal":"앉아를 익혀요","reason":"겁이 많아 기초부터 짰어요",
			"weeks":[{"weekNo":1,"theme":"앉아","milestone":"앉아 기초 해내기","curriculumIds":[11]},
			{"weekNo":2,"theme":"심화","milestone":"앉아 심화 해내기","curriculumIds":[12]}]}
			""";

	@Mock
	private StructuredChatCompleter structuredChatCompleter;

	private EvoLinkPlanGenerator generator() {
		return new EvoLinkPlanGenerator(structuredChatCompleter, JsonMapper.builder().build());
	}

	@Test
	void 프롬프트의_플레이스홀더를_모두_채워_호출한다() {
		given(structuredChatCompleter.complete(any())).willReturn(VALID_JSON);

		generator().generate(context(), "");

		StructuredChatRequest request = captureRequest();
		assertThat(request.userPrompt()).doesNotContain("{{");
		assertThat(request.systemPrompt()).contains("## 페르소나");
		assertThat(request.userPrompt()).contains("이름: 초코")
				.contains("\"curriculumId\":11")
				.contains("\"curriculumId\":12")
				.contains("\"산책할 때 줄을 당겨요\"");
		assertThat(request.schemaName()).isEqualTo("training_plan");
		assertThat(request.schemaJson()).contains("\"curriculumIds\"");
	}

	@Test
	void 재시도_피드백이_있으면_사용자_프롬프트_끝에_덧붙인다() {
		given(structuredChatCompleter.complete(any())).willReturn(VALID_JSON);

		generator().generate(context(), "주차 수가 2가 아닙니다");

		assertThat(captureRequest().userPrompt())
				.endsWith("이전 응답에서 다음 문제가 있었습니다. 고쳐서 다시 만드세요: 주차 수가 2가 아닙니다");
	}

	@Test
	void 응답_JSON을_플랜_초안으로_해석한다() {
		given(structuredChatCompleter.complete(any())).willReturn(VALID_JSON);

		PlanDraft draft = generator().generate(context(), "");

		assertThat(draft.title()).isEqualTo("초코의 2주 앉아 플랜");
		assertThat(draft.goal()).isEqualTo("앉아를 익혀요");
		assertThat(draft.reason()).isEqualTo("겁이 많아 기초부터 짰어요");
		assertThat(draft.weeks()).extracting(PlanDraft.WeekDraft::weekNo).containsExactly(1, 2);
		assertThat(draft.weeks()).extracting(PlanDraft.WeekDraft::curriculumIds)
				.containsExactly(List.of(11L), List.of(12L));
	}

	@Test
	void 스키마에_없는_필드는_무시한다() {
		given(structuredChatCompleter.complete(any())).willReturn(
				"{\"title\":\"t\",\"goal\":\"g\",\"reason\":\"r\",\"extra\":1,\"weeks\":[]}");

		PlanDraft draft = generator().generate(context(), "");

		assertThat(draft.title()).isEqualTo("t");
	}

	@Test
	void JSON이_아닌_응답은_생성_실패로_번역한다() {
		given(structuredChatCompleter.complete(any())).willReturn("플랜을 만들었어요");

		assertThatThrownBy(() -> generator().generate(context(), ""))
				.isInstanceOf(PlanGenerationFailedException.class);
	}

	@Test
	void 모델_호출_예외는_원인을_보존해_생성_실패로_번역한다() {
		StructuredChatCompletionFailedException cause = new StructuredChatCompletionFailedException("호출 실패");
		given(structuredChatCompleter.complete(any())).willThrow(cause);

		assertThatThrownBy(() -> generator().generate(context(), ""))
				.isInstanceOf(PlanGenerationFailedException.class)
				.hasCause(cause);
		then(structuredChatCompleter).should().complete(any());
	}

	private StructuredChatRequest captureRequest() {
		ArgumentCaptor<StructuredChatRequest> captor = ArgumentCaptor.forClass(StructuredChatRequest.class);
		then(structuredChatCompleter).should().complete(captor.capture());
		return captor.getValue();
	}

	private static PlanGenerationContext context() {
		DogProfileResult dog = new DogProfileResult(2L, "초코", null, false, 7, "푸들", "MALE", List.of("겁이 많음"), null,
				"");
		CatalogCurriculum basic = new CatalogCurriculum(11L, "앉아 기초", "설명", 1, false, false,
				List.of(new CatalogLesson(111L, "레슨1", 1, 5)));
		CatalogCurriculum advanced = new CatalogCurriculum(12L, "앉아 심화", "설명", 2, false, false,
				List.of(new CatalogLesson(121L, "레슨1", 1, 5)));
		CatalogTopic topic = new CatalogTopic(1L, "앉아", 1, List.of(basic, advanced));
		CatalogCategory category = new CatalogCategory(1L, "퍼피", 1, EntitlementType.PUPPY, List.of(topic));
		return new PlanGenerationContext(dog, "산책할 때 줄을 당겨요", 10, 3, List.of(), Set.of(),
				new TrainingCatalogResult(List.of(category)), LocalDate.of(2026, 10, 15));
	}
}
