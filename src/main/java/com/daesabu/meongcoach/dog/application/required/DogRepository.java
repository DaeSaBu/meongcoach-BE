package com.daesabu.meongcoach.dog.application.required;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강아지 저장·조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface DogRepository extends JpaRepository<Dog, Long> {

	/**
	 * 사용자에게 해당 상태의 강아지가 있는지 확인한다. 첫 강아지만 선택 상태로 두는 데 쓴다.
	 */
	boolean existsByUserIdAndStatus(Long userId, DogStatus status);

	/**
	 * 사용자의 선택된 강아지를 조회한다. 선택된 강아지가 없으면 빈 Optional을 반환한다.
	 * 선택이 여러 건인 데이터가 남아 있어도 예외 없이 끝나도록 first + id 오름차순으로 조회한다.
	 */
	Optional<Dog> findFirstByUserIdAndStatusOrderByIdAsc(Long userId, DogStatus status);
}
