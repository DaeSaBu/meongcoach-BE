package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.entitlement.application.provided.EntitlementChecker;
import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import com.daesabu.meongcoach.training.application.provided.TrainingCatalogFinder;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCategory;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogLesson;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogTopic;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingCatalogQueryService implements TrainingCatalogFinder {

	private final TrainingCategoryRepository trainingCategoryRepository;
	private final EntitlementChecker entitlementChecker;

	@Override
	public TrainingCatalogResult findCatalog(Long userId) {
		List<TrainingCategory> categories = trainingCategoryRepository.findAllOrdered();
		Map<EntitlementType, Boolean> entitlements = checkEntitlements(userId, categories);

		List<CatalogCategory> catalogCategories = categories.stream()
				.map(category -> toCategory(category, entitlements))
				.toList();
		return new TrainingCatalogResult(catalogCategories);
	}

	private Map<EntitlementType, Boolean> checkEntitlements(Long userId, List<TrainingCategory> categories) {
		Map<EntitlementType, Boolean> entitlements = new HashMap<>();
		categories.stream()
				.flatMap(category -> category.findRequiredEntitlementType().stream())
				.distinct()
				.forEach(type -> entitlements.put(type, entitlementChecker.hasEntitlement(userId, type)));
		return entitlements;
	}

	private CatalogCategory toCategory(TrainingCategory category, Map<EntitlementType, Boolean> entitlements) {
		List<CatalogTopic> topics = category.getTopics().stream()
				.map(topic -> toTopic(topic, entitlements))
				.toList();
		return new CatalogCategory(category.getId(), category.getTitle(), category.getSortOrder(),
				category.getRequiredEntitlementType(), topics);
	}

	private CatalogTopic toTopic(Topic topic, Map<EntitlementType, Boolean> entitlements) {
		List<CatalogCurriculum> curriculums = topic.getCurriculums().stream()
				.map(curriculum -> toCurriculum(curriculum, entitlements))
				.toList();
		return new CatalogTopic(topic.getId(), topic.getTitle(), topic.getSortOrder(), curriculums);
	}

	private CatalogCurriculum toCurriculum(Curriculum curriculum, Map<EntitlementType, Boolean> entitlements) {
		boolean hasEntitlement = curriculum.findRequiredEntitlementType()
				.map(entitlements::get)
				.orElse(true);
		List<CatalogLesson> lessons = curriculum.getLessons().stream()
				.map(this::toLesson)
				.toList();
		return new CatalogCurriculum(curriculum.getId(), curriculum.getTitle(), curriculum.getDescription(),
				curriculum.getSortOrder(), curriculum.isPremium(), curriculum.isLocked(hasEntitlement), lessons);
	}

	private CatalogLesson toLesson(Lesson lesson) {
		return new CatalogLesson(lesson.getId(), lesson.getTitle(), lesson.getSortOrder(),
				lesson.getEstimatedMinutes());
	}
}
