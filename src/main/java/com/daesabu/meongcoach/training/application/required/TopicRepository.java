package com.daesabu.meongcoach.training.application.required;

import com.daesabu.meongcoach.training.domain.Topic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

	List<Topic> findAllByOrderBySortOrderAsc();
}
