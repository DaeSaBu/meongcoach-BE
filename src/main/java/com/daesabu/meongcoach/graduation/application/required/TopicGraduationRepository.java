package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.TopicGraduation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGraduationRepository extends JpaRepository<TopicGraduation, Long> {

	@EntityGraph(attributePaths = "currentSubmission")
	Optional<TopicGraduation> findByUserIdAndTopicId(Long userId, Long topicId);

	/**
	 * 회원이 제출한 적이 있는 모든 토픽의 수료 행을 가장 최근 제출 순으로 읽는다.
	 */
	@EntityGraph(attributePaths = "currentSubmission")
	List<TopicGraduation> findAllByUserIdOrderBySubmittedAtDescIdDesc(Long userId);

}
