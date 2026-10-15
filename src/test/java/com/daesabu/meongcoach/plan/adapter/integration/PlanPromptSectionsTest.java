package com.daesabu.meongcoach.plan.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.plan.application.PlanGenerationContext;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanPromptSectionsTest {

	@Test
	void 강아지_섹션은_이름_월령_견종_성별_성격_몸무게를_담는다() {
		DogProfileResult dog = dog(7, false, new BigDecimal("3.2"));

		String section = PlanPromptSections.dog(dog);

		assertThat(section).isEqualTo("이름: 초코 / 월령: 7개월 / 견종: 푸들 / 성별: 수컷 / 성격: 활발함, 겁이 많음 / 몸무게: 3.2kg");
	}

	@Test
	void 추정_월령은_추정임과_구간을_함께_적는다() {
		DogProfileResult dog = dog(9, true, null);

		String section = PlanPromptSections.dog(dog);

		assertThat(section).contains("월령: 약 9개월(추정 월령, 구간 \"6개월~1살\")");
	}

	@Test
	void 월령이_없으면_알_수_없다고_적는다() {
		DogProfileResult dog = dog(null, false, null);

		String section = PlanPromptSections.dog(dog);

		assertThat(section).contains("월령: 알 수 없음");
	}

	@Test
	void 몸무게가_없으면_몸무게_항목을_뺀다() {
		DogProfileResult dog = dog(7, false, null);

		String section = PlanPromptSections.dog(dog);

		assertThat(section).doesNotContain("몸무게");
	}

	@Test
	void 성격이_없으면_성격_항목을_뺀다() {
		DogProfileResult dog = new DogProfileResult(2L, "초코", null, false, 7, "푸들", "MALE", List.of(), null, "");

		String section = PlanPromptSections.dog(dog);

		assertThat(section).isEqualTo("이름: 초코 / 월령: 7개월 / 견종: 푸들 / 성별: 수컷");
	}

	@Test
	void 암컷은_암컷으로_적는다() {
		DogProfileResult dog = new DogProfileResult(2L, "초코", null, false, 7, "푸들", "FEMALE", List.of(), null, "");

		assertThat(PlanPromptSections.dog(dog)).contains("성별: 암컷");
	}

	@Test
	void 견종이_비었거나_성별을_알_수_없으면_해당_항목을_뺀다() {
		DogProfileResult dog = new DogProfileResult(2L, "초코", null, false, 7, " ", "UNKNOWN", List.of("활발함"), null, "");

		String section = PlanPromptSections.dog(dog);

		assertThat(section).isEqualTo("이름: 초코 / 월령: 7개월 / 성격: 활발함");
	}

	@Test
	void 견종과_성별이_null이어도_항목을_뺀다() {
		DogProfileResult dog = new DogProfileResult(2L, "초코", null, false, 7, null, null, List.of(), null, "");

		assertThat(PlanPromptSections.dog(dog)).isEqualTo("이름: 초코 / 월령: 7개월");
	}

	@Test
	void 보호자_섹션은_기대사항을_따옴표로_감싸고_연습량을_적는다() {
		PlanGenerationContext context = context("산책할 때 줄을 당겨요", List.of(), Set.of(), catalog());

		String section = PlanPromptSections.owner(context);

		assertThat(section).isEqualTo("기대사항: \"산책할 때 줄을 당겨요\" / 하루 연습: 10분 / 주 연습일: 3일");
	}

	@Test
	void 이력_섹션은_배운_토픽과_완료한_커리큘럼_ID를_적는다() {
		PlanGenerationContext context = context("기대사항", List.of("앉아", "산책"), Set.of(111L, 112L, 121L), catalog());

		String section = PlanPromptSections.history(context);

		assertThat(section).isEqualTo("배운 토픽: 앉아, 산책 / 완료한 커리큘럼 ID: [11, 12]");
	}

	@Test
	void 레슨을_일부만_끝낸_커리큘럼은_완료로_세지_않는다() {
		PlanGenerationContext context = context("기대사항", List.of(), Set.of(111L), catalog());

		String section = PlanPromptSections.history(context);

		assertThat(section).endsWith("완료한 커리큘럼 ID: 없음");
	}

	@Test
	void 이력이_없으면_둘_다_없음으로_적는다() {
		PlanGenerationContext context = context("기대사항", List.of(), Set.of(), catalog());

		String section = PlanPromptSections.history(context);

		assertThat(section).isEqualTo("배운 토픽: 없음 / 완료한 커리큘럼 ID: 없음");
	}

	@Test
	void 카탈로그는_커리큘럼마다_JSON_한_줄이고_분은_레슨_합이다() {
		String section = PlanPromptSections.catalog(catalog());

		assertThat(section.lines().toList()).containsExactly(
				"{\"curriculumId\":11,\"stage\":\"퍼피\",\"topic\":\"앉아\",\"title\":\"앉아 기초\","
						+ "\"description\":\"앉아 기초 설명\",\"lessons\":2,\"minutes\":15}",
				"{\"curriculumId\":12,\"stage\":\"퍼피\",\"topic\":\"앉아\",\"title\":\"앉아 심화\","
						+ "\"description\":\"앉아 심화 설명\",\"lessons\":1,\"minutes\":0}");
	}

	@Test
	void 카탈로그에서_잠긴_커리큘럼은_뺀다() {
		String section = PlanPromptSections.catalog(catalog());

		assertThat(section).doesNotContain("\"curriculumId\":99");
	}

	@Test
	void 피드백이_없으면_빈_문자열이다() {
		assertThat(PlanPromptSections.retryFeedback("")).isEmpty();
	}

	@Test
	void 피드백이_있으면_고쳐서_다시_만들라는_안내를_붙인다() {
		String section = PlanPromptSections.retryFeedback("주차 수가 2가 아닙니다");

		assertThat(section).isEqualTo("이전 응답에서 다음 문제가 있었습니다. 고쳐서 다시 만드세요: 주차 수가 2가 아닙니다");
	}

	private static DogProfileResult dog(Integer ageInMonths, boolean estimated, BigDecimal weightKg) {
		return new DogProfileResult(2L, "초코", null, estimated, ageInMonths, "푸들", "MALE",
				List.of("활발함", "겁이 많음"), weightKg, "");
	}

	private static PlanGenerationContext context(String expectation, List<String> priorTopicTitles,
			Set<Long> completedLessonIds, TrainingCatalogResult catalog) {
		return new PlanGenerationContext(dog(7, false, null), expectation, 10, 3, priorTopicTitles,
				completedLessonIds, catalog, LocalDate.of(2026, 10, 15));
	}

	private static TrainingCatalogResult catalog() {
		CatalogCurriculum basic = new CatalogCurriculum(11L, "앉아 기초", "앉아 기초 설명", 1, false, false,
				List.of(new CatalogLesson(111L, "레슨1", 1, 5), new CatalogLesson(112L, "레슨2", 2, 10)));
		CatalogCurriculum advanced = new CatalogCurriculum(12L, "앉아 심화", "앉아 심화 설명", 2, false, false,
				List.of(new CatalogLesson(121L, "레슨1", 1, null)));
		CatalogCurriculum locked = new CatalogCurriculum(99L, "잠긴 커리큘럼", "잠김", 3, true, true,
				List.of(new CatalogLesson(991L, "레슨1", 1, 5)));
		CatalogTopic topic = new CatalogTopic(1L, "앉아", 1, List.of(basic, advanced, locked));
		CatalogCategory category = new CatalogCategory(1L, "퍼피", 1, EntitlementType.PUPPY, List.of(topic));
		return new TrainingCatalogResult(List.of(category));
	}
}
