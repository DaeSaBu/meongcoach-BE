package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LessonMentionTargetReader implements MentionTargetReader {

	private final LessonFinder lessonFinder;

	@Override
	public MentionTargetType targetType() {
		return MentionTargetType.LESSON;
	}

	@Override
	public List<MentionTargetResult> findSummaries(Collection<Long> targetIds) {
		return lessonFinder.findSummaries(targetIds).stream()
				.map(lesson -> new MentionTargetResult(MentionTargetType.LESSON, lesson.id(), lesson.title()))
				.toList();
	}
}
