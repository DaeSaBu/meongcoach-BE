package com.daesabu.meongcoach.progress.application.required;

import com.daesabu.meongcoach.progress.domain.TopicProgress;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {

	Optional<TopicProgress> findByUserId(Long userId);
}
