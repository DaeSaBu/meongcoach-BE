package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
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

	/**
	 * 수료 행을 id로 쓰기 잠금해 읽는다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from TopicGraduation g where g.id = :id")
	Optional<TopicGraduation> findForUpdateById(Long id);

	/**
	 * 회원이 이전에 받은 보상 가운데 가장 늦은 만료 시각을 구한다. 받은 보상이 없으면 빈 값이다.
	 */
	@Query("select max(g.rewardExpiresAt) from TopicGraduation g where g.userId = :userId")
	Optional<Instant> findLatestRewardExpiresAt(Long userId);

	/**
	 * 회원 ID를 키로 PostgreSQL 트랜잭션 advisory lock을 잡는다. 같은 키를 잠근 다른 트랜잭션이 끝날 때까지 기다리고, 잡은 락은 현재 트랜잭션이 끝날 때 풀린다.
	 */
	@Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:userId)", nativeQuery = true)
	void lockByUserId(Long userId);
}
