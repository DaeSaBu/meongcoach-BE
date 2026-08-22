package com.daesabu.meongcoach.dog.adapter.webapi;

import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogListResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogProfileImageResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogResponse;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileImageFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileImageResult;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

	private final DogProfileFinder dogProfileFinder;
	private final DogProfileImageFinder dogProfileImageFinder;

	@GetMapping
	public DogListResponse findDogs(@CurrentUserId Long userId) {
		List<Dog> dogs = dogProfileFinder.findDogs(userId);
		return DogListResponse.from(dogs);
	}

	@GetMapping("/{dogId}")
	public DogResponse findDog(@CurrentUserId Long userId, @PathVariable Long dogId) {
		Dog dog = dogProfileFinder.findDog(userId, dogId);
		return DogResponse.from(dog);
	}

	@GetMapping("/profile/image")
	public DogProfileImageResponse findProfileImage(@CurrentUserId Long userId) {
		DogProfileImageResult profileImage = dogProfileImageFinder.findSelectedProfileImage(userId);
		return DogProfileImageResponse.from(profileImage);
	}
}
