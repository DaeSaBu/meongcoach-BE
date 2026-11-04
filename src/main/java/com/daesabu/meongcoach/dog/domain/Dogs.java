package com.daesabu.meongcoach.dog.domain;

import com.daesabu.meongcoach.dog.domain.exception.DogLimitExceededException;
import com.daesabu.meongcoach.dog.domain.exception.DogNotFoundException;
import com.daesabu.meongcoach.dog.domain.exception.LastDogNotDeletableException;
import com.daesabu.meongcoach.dog.domain.exception.SelectedDogNotDeletableException;
import java.util.List;

public class Dogs {
	public static final int MAX_COUNT_PER_USER = 5;

	private final List<Dog> dogs;

	public Dogs(List<Dog> dogs) {
		this.dogs = List.copyOf(dogs);
	}

	public Dog register(DogRegisterCommand command) {
		if (dogs.size() >= MAX_COUNT_PER_USER) {
			throw new DogLimitExceededException();
		}
		Dog dog = Dog.register(command);
		if (!hasSelected()) {
			dog.select();
		}
		return dog;
	}

	public void delete(Long dogId) {
		Dog dog = find(dogId);
		if (dogs.size() <= 1) {
			throw new LastDogNotDeletableException();
		}
		if (dog.isSelected()) {
			throw new SelectedDogNotDeletableException();
		}
		dog.delete();
	}

	public void select(Long dogId) {
		Dog target = find(dogId);
		dogs.forEach(Dog::unselect);
		target.select();
	}

	private Dog find(Long dogId) {
		return dogs.stream()
				.filter(dog -> dog.getId().equals(dogId))
				.findFirst()
				.orElseThrow(() -> new DogNotFoundException(dogId));
	}

	private boolean hasSelected() {
		return dogs.stream().anyMatch(Dog::isSelected);
	}
}
