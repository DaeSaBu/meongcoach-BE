package com.daesabu.meongcoach.progress.application.required;

import com.daesabu.meongcoach.progress.domain.UserCurriculumProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자별 토픽 진입 기록 조회 리포지토리. Spring Data JPA가 런타임에 구현한다.
 */
public interface UserCurriculumProgressRepository extends JpaRepository<UserCurriculumProgress, Long> {

	/**
	 * 한 사용자가 가장 최근에 진입한 토픽 기록을 조회한다. 기록이 없으면 빈 Optional을 반환한다.
	 */
	Optional<UserCurriculumProgress> findFirstByUserIdOrderByUpdatedAtDesc(Long userId);

	/**
	 * 한 사용자의 특정 토픽 진입 기록을 조회한다. 기록이 없으면 빈 Optional을 반환한다.
	 */
	Optional<UserCurriculumProgress> findByUserIdAndTopicId(Long userId, Long topicId);
}
