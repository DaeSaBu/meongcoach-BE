package com.daesabu.meongcoach.graduation.application.required;

import com.daesabu.meongcoach.graduation.domain.GraduationSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GraduationSubmissionRepository extends JpaRepository<GraduationSubmission, Long> {
	Optional<GraduationSubmission> findFirstByUserIdAndDogIdAndTopicIdOrderByIdDesc(Long userId, Long dogId, Long topicId);

	@Query("""
			select s
			from GraduationSubmission s
			where s.id in (
				select max(submission.id)
				from GraduationSubmission submission
				where submission.userId = :userId and submission.dogId = :dogId
				group by submission.topicId
			)
			order by s.submittedAt desc, s.id desc
			""")
	List<GraduationSubmission> findLatestByUserIdAndDogId(Long userId, Long dogId);
}
