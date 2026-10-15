package com.daesabu.meongcoach.training.application.provided.dto;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record TrainingCatalogResult(List<CatalogCategory> categories) {

	public List<CatalogCurriculum> openCurriculums() {
		return allCurriculums()
				.filter(curriculum -> !curriculum.locked())
				.toList();
	}

	public Optional<CatalogEntry> findCurriculum(Long curriculumId) {
		return categories.stream()
				.flatMap(category -> category.topics().stream()
						.flatMap(topic -> topic.curriculums().stream()
								.map(curriculum -> new CatalogEntry(category, topic, curriculum))))
				.filter(entry -> entry.curriculum().id().equals(curriculumId))
				.findFirst();
	}

	public List<Long> allLessonIds() {
		return allCurriculums()
				.flatMap(curriculum -> curriculum.lessons().stream())
				.map(CatalogLesson::id)
				.toList();
	}

	private Stream<CatalogCurriculum> allCurriculums() {
		return categories.stream()
				.flatMap(category -> category.topics().stream())
				.flatMap(topic -> topic.curriculums().stream());
	}

	public record CatalogCategory(Long id, String title, int sortOrder, EntitlementType requiredEntitlementType,
			List<CatalogTopic> topics) {
	}

	public record CatalogTopic(Long id, String title, int sortOrder, List<CatalogCurriculum> curriculums) {
	}

	public record CatalogCurriculum(Long id, String title, String description, int sortOrder, boolean premium,
			boolean locked, List<CatalogLesson> lessons) {
	}

	public record CatalogLesson(Long id, String title, int sortOrder, Integer estimatedMinutes) {
	}

	public record CatalogEntry(CatalogCategory category, CatalogTopic topic, CatalogCurriculum curriculum) {
	}
}
