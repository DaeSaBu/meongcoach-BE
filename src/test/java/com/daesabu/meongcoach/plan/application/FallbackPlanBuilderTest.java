package com.daesabu.meongcoach.plan.application;

import static com.daesabu.meongcoach.plan.application.CatalogBuilder.allLessonsOf;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.catalog;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.category;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.completed;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.context;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.curriculum;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.dog;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.locked;
import static com.daesabu.meongcoach.plan.application.CatalogBuilder.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.domain.PlanAdjustedReason;
import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.PlanGeneratorType;
import com.daesabu.meongcoach.plan.domain.exception.PlanCatalogEmptyException;
import com.daesabu.meongcoach.plan.domain.exception.PlanTopicNotFoundException;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FallbackPlanBuilderTest {

	private final FallbackPlanBuilder builder = new FallbackPlanBuilder();

	@Test
	void 직접_고른_토픽은_카탈로그_순서대로_재정렬한다() {
		TrainingCatalogResult catalog = twoTopicCatalog();

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(2L, 1L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L, 12L), List.of(21L, 22L));
		assertThat(result.draft().weeks()).extracting(WeekDraft::theme).containsExactly("앉기", "산책");
		assertThat(result.generatorType()).isEqualTo(PlanGeneratorType.MANUAL);
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.adjustedReason()).isNull();
	}

	@Test
	void 입력_카탈로그가_뒤섞여_있어도_카테고리와_토픽_정렬_순서로_재정렬한다() {
		TrainingCatalogResult catalog = catalog(
				category(2L, EntitlementType.JUNIOR,
						topic(4L, "배변", curriculum(41L, "배변 기초", 1, 1)),
						topic(3L, "기다려", curriculum(31L, "기다려 기초", 1, 1))),
				category(1L, EntitlementType.PUPPY,
						topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1)),
						topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(4L, 3L, 2L, 1L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L, 21L), List.of(31L, 41L));
	}

	@Test
	void 완료한_커리큘럼은_뒤로_보낸다() {
		TrainingCatalogResult catalog = twoTopicCatalog();
		Set<Long> done = allLessonsOf(11L, 1);

		GeneratedPlan result = builder.buildManual(context(7, catalog, done), List.of(1L, 2L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(12L, 21L), List.of(22L, 11L));
	}

	@Test
	void 커리큘럼은_sortOrder_순으로_배치한다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(12L, "앉기 심화", 2, 1), curriculum(11L, "앉기 기초", 1, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L), List.of(12L));
	}

	@Test
	void 커리큘럼이_홀수면_앞_주에_하나_더_담는다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1), curriculum(12L, "앉기 심화", 2, 1),
						curriculum(13L, "앉기 응용", 3, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L, 12L), List.of(13L));
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.adjustedReason()).isNull();
	}

	@Test
	void 커리큘럼이_하나면_1주로_줄이고_사유를_남긴다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L));

		assertThat(result.draft().weeks()).hasSize(1);
		assertThat(result.draft().weeks().getFirst().weekNo()).isEqualTo(1);
		assertThat(result.durationWeeks()).isEqualTo(1);
		assertThat(result.adjustedReason()).isEqualTo(PlanAdjustedReason.DURATION_SHORTENED);
		assertThat(result.draft().title()).isEqualTo("초코의 1주 앉기 플랜");
		assertThat(result.draft().goal()).isEqualTo("1주 동안 앉기를 차근차근 익혀요");
	}

	@Test
	void 모두_완료한_커리큘럼이면_복습으로_표시한다() {
		TrainingCatalogResult catalog = twoTopicCatalog();
		Set<Long> done = Set.of(111L, 121L, 211L, 221L);

		GeneratedPlan result = builder.buildManual(context(7, catalog, done), List.of(1L, 2L));

		assertThat(result.adjustedReason()).isEqualTo(PlanAdjustedReason.REVIEW);
		assertThat(result.durationWeeks()).isEqualTo(2);
		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L, 12L), List.of(21L, 22L));
	}

	@Test
	void 잠긴_커리큘럼은_담지_않고_모두_잠긴_토픽은_제외한다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1), locked(12L, "앉기 심화", 2, 1)),
				topic(2L, "산책", locked(21L, "산책 기초", 1, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L, 2L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds).containsExactly(List.of(11L));
		assertThat(result.draft().goal()).isEqualTo("1주 동안 앉기를 차근차근 익혀요");
	}

	@Test
	void 레슨이_없는_커리큘럼은_직접_고르기와_대체_플랜_모두에서_담지_않는다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1), curriculum(12L, "앉기 빈 커리큘럼", 2, 0)),
				topic(2L, "산책", curriculum(21L, "산책 빈 커리큘럼", 1, 0)),
				topic(3L, "배변", curriculum(31L, "배변 기초", 1, 1))));

		GeneratedPlan manual = builder.buildManual(context(7, catalog, Set.of()), List.of(1L, 2L));
		GeneratedPlan fallback = builder.buildFallback(context(7, catalog, Set.of()));

		assertThat(manual.draft().weeks()).extracting(WeekDraft::curriculumIds).containsExactly(List.of(11L));
		assertThat(fallback.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L), List.of(31L));
	}

	@Test
	void 선택한_토픽이_카탈로그에_없으면_토픽_없음_예외를_던진다() {
		TrainingCatalogResult catalog = twoTopicCatalog();

		assertThatThrownBy(() -> builder.buildManual(context(7, catalog, Set.of()), List.of(1L, 99L)))
				.isInstanceOf(PlanTopicNotFoundException.class);
	}

	@Test
	void 선택한_토픽에_열린_커리큘럼이_없으면_카탈로그_비어_있음_예외를_던진다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", locked(11L, "앉기 기초", 1, 1))));

		assertThatThrownBy(() -> builder.buildManual(context(7, catalog, Set.of()), List.of(1L)))
				.isInstanceOf(PlanCatalogEmptyException.class);
	}

	@Test
	void 문구는_이름_토픽_월령_학습량으로_만든다() {
		TrainingCatalogResult catalog = twoTopicCatalog();

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L, 2L));

		assertThat(result.draft().title()).isEqualTo("초코의 2주 앉기 플랜");
		assertThat(result.draft().goal()).isEqualTo("2주 동안 앉기, 산책을 차근차근 익혀요");
		assertThat(result.draft().reason()).isEqualTo(
				"이렇게 짰어요: 앉기, 산책을 7개월 초코에게 맞게 기초부터 단계별로 이어지도록 묶고, 하루 10분 · 주 3일에 맞췄어요.");
		assertThat(result.draft().weeks().get(0).milestone()).isEqualTo("앉기 심화까지 해내기");
		assertThat(result.draft().weeks().get(1).milestone()).isEqualTo("산책 심화까지 해내기");
	}

	@Test
	void 한_주에_여러_토픽이_섞이면_테마를_점으로_잇는다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1)),
				topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1), curriculum(22L, "산책 심화", 2, 1))));

		GeneratedPlan result = builder.buildManual(context(7, catalog, Set.of()), List.of(1L, 2L));

		assertThat(result.draft().weeks()).extracting(WeekDraft::theme).containsExactly("앉기·산책", "산책");
	}

	@Test
	void 추정한_월령은_이유_문구에_추정으로_표시한다() {
		TrainingCatalogResult catalog = twoTopicCatalog();
		PlanGenerationContext context = new PlanGenerationContext(dog(9, true), null, 10, 3, List.of(), Set.of(),
				catalog, CatalogBuilder.TODAY);

		GeneratedPlan result = builder.buildManual(context, List.of(1L));

		assertThat(result.draft().reason()).contains("약 9개월(추정) 초코에게");
	}

	@Test
	void 월령을_모르면_이유_문구에서_월령을_뺀다() {
		TrainingCatalogResult catalog = twoTopicCatalog();

		GeneratedPlan result = builder.buildManual(context(null, catalog, Set.of()), List.of(1L));

		assertThat(result.draft().reason()).contains("앉기를 초코에게 맞게");
	}

	@Test
	void 대체_플랜은_월령에_맞는_카테고리의_토픽_두_개를_고른다() {
		TrainingCatalogResult catalog = catalog(
				category(1L, EntitlementType.ADULT,
						topic(1L, "어덜트토픽", curriculum(11L, "어덜트 기초", 1, 1))),
				category(2L, EntitlementType.PUPPY,
						topic(2L, "앉기", curriculum(21L, "앉기 기초", 1, 1)),
						topic(3L, "산책", curriculum(31L, "산책 기초", 1, 1)),
						topic(4L, "배변", curriculum(41L, "배변 기초", 1, 1))));

		GeneratedPlan result = builder.buildFallback(context(7, catalog, Set.of()));

		assertThat(result.generatorType()).isEqualTo(PlanGeneratorType.FALLBACK);
		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(21L), List.of(31L));
		assertThat(result.draft().goal()).isEqualTo("2주 동안 앉기, 산책을 차근차근 익혀요");
	}

	@Test
	void 시니어는_시니어_카테고리가_없으면_어덜트_카테고리를_먼저_쓴다() {
		TrainingCatalogResult catalog = catalog(
				category(1L, EntitlementType.PUPPY,
						topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1))),
				category(2L, EntitlementType.JUNIOR,
						topic(2L, "주니어토픽", curriculum(21L, "주니어 기초", 1, 1))),
				category(3L, EntitlementType.ADULT,
						topic(3L, "어덜트토픽", curriculum(31L, "어덜트 기초", 1, 1))));

		GeneratedPlan result = builder.buildFallback(context(120, catalog, Set.of()));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(31L), List.of(21L));
	}

	@Test
	void 월령에_맞는_카테고리가_모자라면_앞_단계_역순_다음_나머지_순으로_채운다() {
		TrainingCatalogResult catalog = catalog(
				category(1L, EntitlementType.SENIOR,
						topic(1L, "시니어토픽", curriculum(11L, "시니어 기초", 1, 1))),
				category(2L, EntitlementType.PUPPY,
						topic(2L, "퍼피토픽", curriculum(21L, "퍼피 기초", 1, 1))),
				category(3L, EntitlementType.ADULT,
						topic(3L, "어덜트토픽", curriculum(31L, "어덜트 기초", 1, 1))));

		GeneratedPlan result = builder.buildFallback(context(15, catalog, Set.of()));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(21L), List.of(31L));
	}

	@Test
	void 대체_플랜은_미완료_커리큘럼이_있는_토픽만_고른다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1)),
				topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1)),
				topic(3L, "배변", curriculum(31L, "배변 기초", 1, 1))));

		GeneratedPlan result = builder.buildFallback(context(7, catalog, Set.of(111L)));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(21L), List.of(31L));
		assertThat(result.adjustedReason()).isNull();
	}

	@Test
	void 대체_플랜에_담을_미완료_토픽이_없으면_복습으로_구성한다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1)),
				topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1)),
				topic(3L, "배변", curriculum(31L, "배변 기초", 1, 1))));

		GeneratedPlan result = builder.buildFallback(context(7, catalog, completed(111L, 211L, 311L)));

		assertThat(result.adjustedReason()).isEqualTo(PlanAdjustedReason.REVIEW);
		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(11L), List.of(21L));
	}

	@Test
	void 대체_플랜은_잠긴_토픽을_고르지_않는다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", locked(11L, "앉기 기초", 1, 1)),
				topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1), curriculum(22L, "산책 심화", 2, 1))));

		GeneratedPlan result = builder.buildFallback(context(7, catalog, Set.of()));

		assertThat(result.draft().weeks()).extracting(WeekDraft::curriculumIds)
				.containsExactly(List.of(21L), List.of(22L));
	}

	@Test
	void 열린_커리큘럼이_하나도_없으면_카탈로그_비어_있음_예외를_던진다() {
		TrainingCatalogResult catalog = catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", locked(11L, "앉기 기초", 1, 1))));

		assertThatThrownBy(() -> builder.buildFallback(context(7, catalog, Set.of())))
				.isInstanceOf(PlanCatalogEmptyException.class);
	}

	private static TrainingCatalogResult twoTopicCatalog() {
		return catalog(category(1L, EntitlementType.PUPPY,
				topic(1L, "앉기", curriculum(11L, "앉기 기초", 1, 1), curriculum(12L, "앉기 심화", 2, 1)),
				topic(2L, "산책", curriculum(21L, "산책 기초", 1, 1), curriculum(22L, "산책 심화", 2, 1))));
	}
}
