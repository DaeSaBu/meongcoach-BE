package com.daesabu.meongcoach.dog.application.required;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강아지 저장·조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface DogRepository extends JpaRepository<Dog, Long> {

	/**
	 * 사용자의 선택된 강아지 중 id가 가장 작은 하나를 조회한다. 선택된 강아지가 없으면 빈 Optional을 반환한다.
	 */
	Optional<Dog> findFirstByUserIdAndStatusOrderByIdAsc(Long userId, DogStatus status);

	/**
	 * 사용자의 강아지를 등록 순(id 오름차순)으로 모두 조회한다. 없으면 빈 리스트를 반환한다.
	 * 소프트 딜리트된 강아지는 제외한다.
	 */
	List<Dog> findAllByUserIdOrderByIdAsc(Long userId);

	/**
	 * 강아지 ID와 소유자가 모두 일치할 때만 조회한다. 다른 사용자의 강아지면 빈 Optional을 반환한다.
	 */
	Optional<Dog> findByIdAndUserId(Long id, Long userId);
}
