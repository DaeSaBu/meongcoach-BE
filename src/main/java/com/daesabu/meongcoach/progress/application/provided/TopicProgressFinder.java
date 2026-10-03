package com.daesabu.meongcoach.progress.application.provided;

import java.util.Optional;

public interface TopicProgressFinder {

	Optional<Long> findLatestTopicId(Long userId);
}
