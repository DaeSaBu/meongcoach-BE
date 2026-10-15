package com.daesabu.meongcoach.plan.application;

import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingCatalogResult.CatalogCurriculum;
import java.util.List;

public final class PlanCatalogRules {

	private PlanCatalogRules() {
	}

	public static boolean isPlannable(CatalogCurriculum curriculum) {
		return !curriculum.locked() && !curriculum.lessons().isEmpty();
	}

	public static List<CatalogCurriculum> plannableCurriculums(TrainingCatalogResult catalog) {
		return catalog.openCurriculums().stream()
				.filter(PlanCatalogRules::isPlannable)
				.toList();
	}
}
