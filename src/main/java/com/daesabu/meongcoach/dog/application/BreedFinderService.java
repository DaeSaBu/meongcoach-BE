package com.daesabu.meongcoach.dog.application;

import com.daesabu.meongcoach.dog.application.provided.BreedFinder;
import com.daesabu.meongcoach.dog.domain.shared.Breed;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class BreedFinderService implements BreedFinder {

	private static final List<Breed> ORDERED = order();

	@Override
	public List<Breed> findAll() {
		return ORDERED;
	}

	private static List<Breed> order() {
		Stream<Breed> mixed = Stream.of(Breed.MIXED);
		Stream<Breed> others = Arrays.stream(Breed.values())
				.filter(breed -> breed != Breed.MIXED)
				.sorted(Comparator.comparing(Breed::getLabel));
		return Stream.concat(mixed, others).toList();
	}
}
