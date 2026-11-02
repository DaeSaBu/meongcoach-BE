package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.dto.MentionLevelResult;
import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.training.application.provided.LessonFinder;
import com.daesabu.meongcoach.training.application.provided.dto.LessonSummaryResult;
import com.daesabu.meongcoach.training.application.provided.dto.TrainingContentResult;
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
				.map(LessonMentionTargetReader::toResult)
				.toList();
	}

	private static MentionTargetResult toResult(LessonSummaryResult lesson) {
		MentionLevelResult category = toLevel(lesson.category());
		MentionLevelResult topic = toLevel(lesson.topic());
		MentionLevelResult curriculum = toLevel(lesson.curriculum());
		MentionLevelResult lessonLevel = new MentionLevelResult(lesson.id(), lesson.title());
		return new MentionTargetResult(MentionTargetType.LESSON, lesson.id(), category, topic, curriculum, lessonLevel);
	}

	private static MentionLevelResult toLevel(TrainingContentResult content) {
		return new MentionLevelResult(content.id(), content.title());
	}
}
