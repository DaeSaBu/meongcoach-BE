package com.daesabu.meongcoach.dog.application.required;

import com.daesabu.meongcoach.dog.domain.Dog;
import com.daesabu.meongcoach.dog.domain.DogStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 강아지 저장·조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface DogRepository extends JpaRepository<Dog, Long> {

	/**
	 * 사용자의 선택된 강아지를 조회한다. 선택된 강아지가 없으면 빈 Optional을 반환한다.
	 * 선택이 여러 건인 데이터가 남아 있어도 예외 없이 끝나도록 first + id 오름차순으로 조회한다.
	 */
	Optional<Dog> findFirstByUserIdAndStatusOrderByIdAsc(Long userId, DogStatus status);

	/**
	 * 사용자의 강아지를 등록 순(id 오름차순)으로 모두 조회한다. 없으면 빈 리스트를 반환한다.
	 * 성격 컬렉션은 adapter가 응답으로 내리므로 트랜잭션 안에서 함께 로딩한다. (Hibernate가 루트 엔티티 중복을 제거한다)
	 * 사용자 단위 규칙을 판단하는 {@code Dogs}의 재료로도 쓴다. 소프트 딜리트된 강아지는 엔티티의 {@code @SQLRestriction}으로 자동 제외된다.
	 */
	@EntityGraph(attributePaths = "personalities")
	List<Dog> findAllByUserIdOrderByIdAsc(Long userId);

	/**
	 * 강아지 ID와 소유자가 모두 일치할 때만 성격과 함께 조회한다.
	 * 남의 강아지는 결과 없음으로 처리해 존재 여부를 숨긴다.
	 */
	@EntityGraph(attributePaths = "personalities")
	Optional<Dog> findByIdAndUserId(Long id, Long userId);
}
