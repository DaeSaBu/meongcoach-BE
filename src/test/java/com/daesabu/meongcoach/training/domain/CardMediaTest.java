package com.daesabu.meongcoach.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CardMedia 도메인")
class CardMediaTest {

	@Test
	@DisplayName("생성하면 카드와 미디어 타입이 설정된다")
	void createSetsCardAndMediaType() {
		TrainingCategory category = TrainingCategory.create("기본 훈련", 1);
		Topic topic = Topic.create(category, "산책 훈련", 1);
		Curriculum curriculum = Curriculum.create(topic, "리드줄 적응", 1, null, null);
		Lesson lesson = Lesson.create(curriculum, "리드줄 보여주기", 1, 5);
		Card card = Card.create(lesson, "리드줄 냄새 맡게 하기", 1, null);

		CardMedia media = CardMedia.create(card, MediaType.VIDEO, "https://cdn.meongcoach.com/video.mp4", 1);

		assertThat(media.getCard()).isEqualTo(card);
		assertThat(media.getMediaType()).isEqualTo(MediaType.VIDEO);
		assertThat(media.getUrl()).isEqualTo("https://cdn.meongcoach.com/video.mp4");
		assertThat(media.getSortOrder()).isEqualTo(1);
	}
}
