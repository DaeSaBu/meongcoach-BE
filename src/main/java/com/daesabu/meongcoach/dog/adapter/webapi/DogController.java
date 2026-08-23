package com.daesabu.meongcoach.dog.adapter.webapi;

import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogListResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogProfileImageResponse;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogProfileUpdateRequest;
import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogResponse;
import com.daesabu.meongcoach.dog.application.provided.DogProfileDeleter;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.application.provided.DogProfileUpdater;
import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

	private final DogProfileFinder dogProfileFinder;
	private final DogProfileUpdater dogProfileUpdater;
	private final DogProfileDeleter dogProfileDeleter;

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

	// 수정 화면이 단건 조회 값을 초기값으로 받아 전부 다시 보내므로 PUT 전체 교체로 둔다
	@PutMapping("/{dogId}")
	public DogResponse updateDog(@CurrentUserId Long userId, @PathVariable Long dogId,
	                             @Valid @RequestBody DogProfileUpdateRequest request) {
		Dog dog = dogProfileUpdater.update(userId, dogId, request.toCommand());
		return DogResponse.from(dog);
	}

	// 소프트 딜리트라 삭제 후에도 행은 남지만, 클라이언트 관점에서는 리소스가 사라지므로 204로 응답한다
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
