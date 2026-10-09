package com.daesabu.meongcoach.training.application;

import com.daesabu.meongcoach.training.application.provided.TrainingCategoryFinder;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainingCategoryQueryService implements TrainingCategoryFinder {

	private final TrainingCategoryRepository trainingCategoryRepository;

	@Override
	public List<TrainingCategory> findAllOrdered() {
		return trainingCategoryRepository.findAllOrdered();
	}
}
