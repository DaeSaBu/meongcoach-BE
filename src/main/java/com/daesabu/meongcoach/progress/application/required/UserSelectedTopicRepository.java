package com.daesabu.meongcoach.progress.application.required;

import com.daesabu.meongcoach.progress.domain.UserSelectedTopic;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자별 선택 토픽 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface UserSelectedTopicRepository extends JpaRepository<UserSelectedTopic, Long> {

	/**
	 * 한 사용자가 마지막으로 선택한 토픽 기록을 조회한다. 기록이 없으면 빈 Optional을 반환한다.
	 */
	Optional<UserSelectedTopic> findByUserId(Long userId);
}
