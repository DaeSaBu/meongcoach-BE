package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.TopicSelection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicSelectionRepository extends JpaRepository<TopicSelection, Long> {

	Optional<TopicSelection> findByUserId(Long userId);
}
