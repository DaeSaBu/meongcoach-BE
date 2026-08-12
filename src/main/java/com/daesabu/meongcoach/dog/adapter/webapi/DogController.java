package com.daesabu.meongcoach.dog.adapter.webapi;

import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogProfileImageResponse;
import com.daesabu.meongcoach.dog.application.provided.DogProfileImageFinder;
import com.daesabu.meongcoach.shared.security.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

	private final DogProfileImageFinder dogProfileImageFinder;

	@GetMapping("/{dogId}/profile-image")
	public DogProfileImageResponse findProfileImage(@CurrentUserId Long userId, @PathVariable Long dogId) {
		String profileImageUrl = dogProfileImageFinder.findProfileImageUrl(userId, dogId);
		return DogProfileImageResponse.from(profileImageUrl);
	}
}
