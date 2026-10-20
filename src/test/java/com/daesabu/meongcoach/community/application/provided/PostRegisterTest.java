package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.MentionRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.exception.InvalidMentionTargetTypeException;
import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CardRepository;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
import com.daesabu.meongcoach.training.domain.Card;
import com.daesabu.meongcoach.training.domain.CardFixture;
import com.daesabu.meongcoach.training.domain.Curriculum;
import com.daesabu.meongcoach.training.domain.CurriculumFixture;
import com.daesabu.meongcoach.training.domain.Lesson;
import com.daesabu.meongcoach.training.domain.LessonFixture;
import com.daesabu.meongcoach.training.domain.Topic;
import com.daesabu.meongcoach.training.domain.TopicFixture;
import com.daesabu.meongcoach.training.domain.TrainingCategory;
import com.daesabu.meongcoach.training.domain.TrainingCategoryFixture;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PostRegisterTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private PostRegister postRegister;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private TrainingCategoryRepository trainingCategoryRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CurriculumRepository curriculumRepository;

	@Autowired
	private LessonRepository lessonRepository;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 멘션_없이_게시글을_등록한다() {
		Post registered = postRegister.register(USER_ID, new PostRegisterRequest("본문", null));
		flushAndClear();

		Post found = postRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.getUserId()).isEqualTo(USER_ID);
		assertThat(found.getContent().value()).isEqualTo("본문");
		assertThat(found.getMention()).isNull();
	}

	@Test
	void 카드를_멘션한_게시글을_등록한다() {
		Card card = saveCard("앉아 준비");

		Post registered = postRegister.register(USER_ID,
				new PostRegisterRequest("본문", new MentionRequest("CARD", card.getId())));
		flushAndClear();

		Post found = postRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.findMentionTargetId(MentionTargetType.CARD)).contains(card.getId());
	}

	@Test
	void 멘션한_카드가_없으면_예외를_던지고_저장하지_않는다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("CARD", 999_999L));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(MentionTargetNotFoundException.class);
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 지원하지_않는_멘션_대상_종류면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("LESSON", 1L));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMentionTargetTypeException.class);
	}

	@Test
	void 본문이_비어_있으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest(" ", null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 멘션에_targetType이_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest(null, 1L));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 멘션에_targetId가_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("CARD", null));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	private Card saveCard(String title) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create("기본 카테고리", 1, null, null));
		Topic topic = topicRepository.save(TopicFixture.create(category, "기본 토픽", 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(CurriculumFixture.create(topic, "기본 커리큘럼", 1, null, null));
		Lesson lesson = lessonRepository.save(LessonFixture.create(curriculum, "기본 레슨", 1, 5));
		return cardRepository.save(CardFixture.create(lesson, title, 1, "지시문"));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
