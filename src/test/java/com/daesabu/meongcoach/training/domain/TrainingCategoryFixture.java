package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.entitlement.domain.shared.EntitlementType;
import java.util.Objects;
import org.springframework.test.util.ReflectionTestUtils;

public final class TrainingCategoryFixture {

	private TrainingCategoryFixture() {
	}

	public static TrainingCategory create(String title, int sortOrder, String description, String iconUrl) {
		return create(title, sortOrder, description, iconUrl, null);
	}

	public static TrainingCategory create(String title, int sortOrder, String description, String iconUrl,
			EntitlementType requiredEntitlementType) {
		TrainingCategory category = new TrainingCategory();
		ReflectionTestUtils.setField(category, "title", title);
		ReflectionTestUtils.setField(category, "sortOrder", sortOrder);
		ReflectionTestUtils.setField(category, "description", Objects.requireNonNullElse(description, ""));
		ReflectionTestUtils.setField(category, "iconUrl", Objects.requireNonNullElse(iconUrl, ""));
		ReflectionTestUtils.setField(category, "requiredEntitlementType", requiredEntitlementType);
		return category;
	}
}
