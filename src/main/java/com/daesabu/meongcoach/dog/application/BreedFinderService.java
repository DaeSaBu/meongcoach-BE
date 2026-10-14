package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.domain.shared.RegisteredBreed;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BreedFinderService implements BreedFinder {

	private static final List<RegisteredBreed> BREEDS = List.of(RegisteredBreed.values());

	@Override
	public List<RegisteredBreed> findAll() {
		return BREEDS;
	}
}
