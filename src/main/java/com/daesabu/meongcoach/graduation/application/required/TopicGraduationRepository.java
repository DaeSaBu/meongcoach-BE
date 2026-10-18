package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TopicGraduationRepository extends JpaRepository<TopicGraduation, Long> {

	Optional<TopicGraduation> findByUserIdAndTopicId(Long userId, Long topicId);

	/**
	 * 회원이 제출한 적이 있는 모든 토픽의 수료 행을 가장 최근 제출 순으로 읽는다.
	 */
	List<TopicGraduation> findAllByUserIdOrderBySubmittedAtDescIdDesc(Long userId);

	/**
	 * 회원·토픽의 수료 행을 쓰기 잠금으로 읽는다. 같은 행을 잠근 다른 트랜잭션이 끝날 때까지 기다린다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from TopicGraduation g where g.userId = :userId and g.topicId = :topicId")
	Optional<TopicGraduation> findForUpdate(Long userId, Long topicId);
}
