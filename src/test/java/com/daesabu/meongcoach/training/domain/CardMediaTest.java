package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardMediaTest {

	@Test
	void 생성하면_카드와_미디어_타입이_설정된다() {
		TrainingCategory category = TrainingCategory.create("기본 교육", 1, null, null);
		Topic topic = Topic.create(category, new TopicCreateCommand("산책 교육", 1, null, null, null));
		Curriculum curriculum = Curriculum.create(topic, new CurriculumCreateCommand("리드줄 적응", 1, null, null));
		Lesson lesson = Lesson.create(curriculum, new LessonCreateCommand("리드줄 보여주기", 1, 5));
		Card card = Card.create(lesson, new CardCreateCommand("리드줄 냄새 맡게 하기", 1, null));

		CardMedia media = CardMedia.create(card,
				new CardMediaCreateCommand(MediaType.VIDEO, "https://cdn.meongcoach.com/video.mp4", 1));

		assertThat(media.getCard()).isEqualTo(card);
		assertThat(media.getMediaType()).isEqualTo(MediaType.VIDEO);
		assertThat(media.getUrl()).isEqualTo("https://cdn.meongcoach.com/video.mp4");
		assertThat(media.getSortOrder()).isEqualTo(1);
	}
}
