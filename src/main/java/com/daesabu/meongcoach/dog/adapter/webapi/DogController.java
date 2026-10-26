package com.daesabu.meongcoach.dog.adapter.webapi;

import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogListResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogProfileImageResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogResponse;
import com.daesabu.meongcoach.dog.application.provided.DogProfileDeleter;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogRegister;
import com.daesabu.meongcoach.dog.application.provided.dto.DogProfileUpdateRequest;
import com.daesabu.meongcoach.dog.application.provided.dto.DogRegisterRequest;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

	private final DogRegister dogRegister;
	private final DogProfileFinder dogProfileFinder;
	private final DogProfileDeleter dogProfileDeleter;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DogResponse registerDog(@CurrentUserId Long userId, @Valid @RequestBody DogRegisterRequest request) {
		Long dogId = dogRegister.register(userId, request);


		Dog dog = dogProfileFinder.findDog(userId, dogId);
		return DogResponse.from(dog);
	}

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

	@PutMapping("/{dogId}")
	public DogResponse updateDog(@CurrentUserId Long userId, @PathVariable Long dogId,
	                             @Valid @RequestBody DogProfileUpdateRequest request) {
		Dog dog = dogRegister.update(userId, dogId, request);
		return DogResponse.from(dog);
	}

	@DeleteMapping("/{dogId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDog(@CurrentUserId Long userId, @PathVariable Long dogId) {
		dogProfileDeleter.delete(userId, dogId);
	}

	@GetMapping("/profile/image")
	public DogProfileImageResponse findProfileImage(@CurrentUserId Long userId) {
		Dog dog = dogProfileFinder.findSelectedDog(userId);
		return DogProfileImageResponse.from(dog);
	}
}
