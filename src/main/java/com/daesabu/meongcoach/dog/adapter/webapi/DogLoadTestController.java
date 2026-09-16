package com.daesabu.meongcoach.dog.adapter.webapi;

import com.daesabu.meongcoach.dog.adapter.webapi.dto.DogListResponse;
import com.daesabu.meongcoach.dog.application.provided.DogProfileFinder;
import com.daesabu.meongcoach.dog.domain.Dog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JMeter 부하 측정용 임시 API. 인증 없이 회원 ID를 쿼리 파라미터로 받아 DogController와 같은 조회 로직을 실행한다.
 * 측정이 끝나면 SecurityConfig의 /api/test/** 허용과 함께 제거한다.
 */
@RestController
@RequestMapping("/api/test/dogs")
@RequiredArgsConstructor
public class DogLoadTestController {

	private final DogProfileFinder dogProfileFinder;

	@GetMapping
	public DogListResponse findDogs(@RequestParam Long userId) {
		List<Dog> dogs = dogProfileFinder.findDogs(userId);
		return DogListResponse.from(dogs);
	}
}
