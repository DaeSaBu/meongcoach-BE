package com.daesabu.meongcoach.dog.application.required;

import com.daesabu.meongcoach.dog.domain.Dog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강아지 저장·조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface DogRepository extends JpaRepository<Dog, Long> {

	/**
	 * 강아지 ID와 소유자가 모두 일치할 때만 조회한다.
	 * 남의 강아지는 결과 없음으로 처리해 존재 여부를 숨긴다.
	 */
	Optional<Dog> findByIdAndUserId(Long id, Long userId);
}
