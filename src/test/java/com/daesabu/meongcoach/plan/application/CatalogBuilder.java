package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.dog.application.provided.DogProfileResult;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

final class CatalogBuilder {

	static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

	private CatalogBuilder() {
	}

	static TrainingCatalogResult catalog(CatalogCategory... categories) {
		return new TrainingCatalogResult(List.of(categories));
	}

	static CatalogCategory category(long id, EntitlementType type, CatalogTopic... topics) {
		return new CatalogCategory(id, "카테고리" + id, (int) id, type, List.of(topics));
	}

	static CatalogTopic topic(long id, String title, CatalogCurriculum... curriculums) {
		return new CatalogTopic(id, title, (int) id, List.of(curriculums));
	}

	static CatalogCurriculum curriculum(long id, String title, int sortOrder, int lessonCount) {
		return curriculum(id, title, sortOrder, false, lessonCount);
	}

	static CatalogCurriculum locked(long id, String title, int sortOrder, int lessonCount) {
		return curriculum(id, title, sortOrder, true, lessonCount);
	}

	static CatalogCurriculum curriculum(long id, String title, int sortOrder, boolean locked, int lessonCount) {
		List<CatalogLesson> lessons = IntStream.rangeClosed(1, lessonCount)
				.mapToObj(order -> new CatalogLesson(lessonId(id, order), title + " 레슨" + order, order, 5))
				.toList();
		return new CatalogCurriculum(id, title, title + " 설명", sortOrder, locked, locked, lessons);
	}

	static long lessonId(long curriculumId, int order) {
		return curriculumId * 10 + order;
	}

	static Set<Long> completed(long... lessonIds) {
		return java.util.Arrays.stream(lessonIds).boxed().collect(java.util.stream.Collectors.toSet());
	}

	static Set<Long> allLessonsOf(long curriculumId, int lessonCount) {
		return IntStream.rangeClosed(1, lessonCount)
				.mapToObj(order -> lessonId(curriculumId, order))
				.collect(java.util.stream.Collectors.toSet());
	}

	static DogProfileResult dog(Integer ageInMonths, boolean estimated) {
		return new DogProfileResult(2L, "초코", null, estimated, ageInMonths, "푸들", "MALE", List.of(), null, "");
	}

	static PlanGenerationContext context(Integer ageInMonths, TrainingCatalogResult catalog,
			Set<Long> completedLessonIds) {
		return new PlanGenerationContext(dog(ageInMonths, false), null, 10, 3, List.of(), completedLessonIds, catalog,
				TODAY);
	}
}
