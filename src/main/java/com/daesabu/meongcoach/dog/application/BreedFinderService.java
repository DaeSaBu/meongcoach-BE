package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 강아지 견종 목록을 조회한다.
 */
@Service
public class BreedFinderService implements BreedFinder {

	@Override
	public List<Breed> findAll() {
		return List.of(Breed.values());
	}
}
