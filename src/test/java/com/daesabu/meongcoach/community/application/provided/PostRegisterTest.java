package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.community.application.provided.dto.MentionRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostMediaRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostRegisterRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostUpdateRequest;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostMedia;
import com.daesabu.meongcoach.community.domain.PostMediaCommand;
import com.daesabu.meongcoach.community.domain.PostMediaType;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.InvalidMentionTargetTypeException;
import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import com.daesabu.meongcoach.community.domain.exception.InvalidPostMediaTypeException;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
import com.daesabu.meongcoach.media.domain.exception.InvalidMediaUrlException;
import com.daesabu.meongcoach.support.ApplicationTest;
import com.daesabu.meongcoach.training.application.required.CurriculumRepository;
import com.daesabu.meongcoach.training.application.required.LessonRepository;
import com.daesabu.meongcoach.training.application.required.TopicRepository;
import com.daesabu.meongcoach.training.application.required.TrainingCategoryRepository;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PostRegisterTest {

	private static final Long USER_ID = 42L;
	private static final String IMAGE_URL = "https://images.test.meongcoach.com/images/community-post/42/photo.jpg";
	private static final String VIDEO_URL = "https://images.test.meongcoach.com/videos/community-post/42/clip.mp4";

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
	private EntityManager entityManager;

	@Test
	void 멘션_없이_게시글을_등록한다() {
		Post registered = postRegister.register(USER_ID, new PostRegisterRequest("본문", null, null));
		flushAndClear();

		Post found = postRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.getUserId()).isEqualTo(USER_ID);
		assertThat(found.getContent().value()).isEqualTo("본문");
		assertThat(found.getMention()).isNull();
	}

	@Test
	void 레슨을_멘션한_게시글을_등록한다() {
		Lesson lesson = saveLesson("앉아 기초");

		Post registered = postRegister.register(USER_ID,
				new PostRegisterRequest("본문", new MentionRequest("LESSON", lesson.getId()), null));
		flushAndClear();

		PostMention mention = postRepository.findById(registered.getId()).orElseThrow().getMention();
		assertThat(mention.getTargetType()).isEqualTo(MentionTargetType.LESSON);
		assertThat(mention.getTargetId()).isEqualTo(lesson.getId());
	}

	@Test
	void 멘션한_레슨이_없으면_예외를_던지고_저장하지_않는다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("LESSON", 999_999L), null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(MentionTargetNotFoundException.class);
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 지원하지_않는_멘션_대상_종류면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("CARD", 1L), null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMentionTargetTypeException.class);
	}

	@Test
	void 본문이_비어_있으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest(" ", null, null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 멘션에_targetType이_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest(null, 1L), null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 멘션에_targetId가_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", new MentionRequest("LESSON", null), null);

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 미디어를_보낸_순서대로_함께_저장한다() {
		List<PostMediaRequest> media = List.of(
				new PostMediaRequest("VIDEO", VIDEO_URL),
				new PostMediaRequest("image", IMAGE_URL));

		Post registered = postRegister.register(USER_ID, new PostRegisterRequest("본문", null, media));
		flushAndClear();

		Post found = postRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.getMedia())
				.extracting(PostMedia::getType, PostMedia::getUrl, PostMedia::getSortOrder)
				.containsExactly(
						tuple(PostMediaType.VIDEO, VIDEO_URL, 0),
						tuple(PostMediaType.IMAGE, IMAGE_URL, 1));
	}

	@Test
	void 미디어를_생략하면_미디어_없이_저장한다() {
		Post registered = postRegister.register(USER_ID, new PostRegisterRequest("본문", null, null));
		flushAndClear();

		Post found = postRepository.findById(registered.getId()).orElseThrow();
		assertThat(found.getMedia()).isEmpty();
	}

	@Test
	void 지원하지_않는_미디어_종류면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("AUDIO", IMAGE_URL)));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidPostMediaTypeException.class);
	}

	@Test
	void 사진이_3장을_넘으면_예외를_던지고_저장하지_않는다() {
		PostMediaRequest image = new PostMediaRequest("IMAGE", IMAGE_URL);
		PostRegisterRequest request = new PostRegisterRequest("본문", null, List.of(image, image, image, image));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(PostMediaCountExceededException.class);
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 우리_스토리지의_URL이_아닌_미디어면_예외를_던지고_저장하지_않는다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", "https://evil.example.com/images/community-post/42/a.jpg")));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMediaUrlException.class);
		assertThat(postRepository.count()).isZero();
	}

	@Test
	void 다른_사용자가_올린_미디어면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", "https://images.test.meongcoach.com/images/community-post/7/a.jpg")));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 게시글_경로가_아닌_미디어면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", "https://images.test.meongcoach.com/images/user-profile/42/a.jpg")));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 미디어_종류와_URL의_종류가_다르면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", VIDEO_URL)));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 미디어에_url이_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", " ")));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 미디어에_type이_없으면_예외를_던진다() {
		PostRegisterRequest request = new PostRegisterRequest("본문", null,
				List.of(new PostMediaRequest(null, IMAGE_URL)));

		assertThatThrownBy(() -> postRegister.register(USER_ID, request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 게시글을_수정하면_본문과_멘션과_미디어를_전체_교체한다() {
		Post post = savePost(USER_ID, null, List.of(new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL)));
		Lesson lesson = saveLesson("앉아 기초");
		PostUpdateRequest request = new PostUpdateRequest("수정한 본문", new MentionRequest("LESSON", lesson.getId()),
				List.of(new PostMediaRequest("VIDEO", VIDEO_URL)));

		postRegister.update(USER_ID, post.getId(), request);
		flushAndClear();

		Post found = postRepository.findById(post.getId()).orElseThrow();
		assertThat(found.getContent().value()).isEqualTo("수정한 본문");
		assertThat(found.getMention().getTargetType()).isEqualTo(MentionTargetType.LESSON);
		assertThat(found.getMention().getTargetId()).isEqualTo(lesson.getId());
		assertThat(found.getMedia())
				.extracting(PostMedia::getType, PostMedia::getUrl, PostMedia::getSortOrder)
				.containsExactly(tuple(PostMediaType.VIDEO, VIDEO_URL, 0));
	}

	@Test
	void 수정할_때_멘션과_미디어를_생략하면_모두_제거한다() {
		Lesson lesson = saveLesson("앉아 기초");
		Post post = savePost(USER_ID, new MentionCommand(MentionTargetType.LESSON, lesson.getId()),
				List.of(new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL)));

		postRegister.update(USER_ID, post.getId(), new PostUpdateRequest("본문", null, null));
		flushAndClear();

		Post found = postRepository.findById(post.getId()).orElseThrow();
		assertThat(found.getMention()).isNull();
		assertThat(found.getMedia()).isEmpty();
	}

	@Test
	void 수정할_때_기존_미디어를_다시_보내면_보낸_순서대로_저장한다() {
		Post post = savePost(USER_ID, null, List.of(
				new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL),
				new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL)));
		List<PostMediaRequest> media = List.of(
				new PostMediaRequest("VIDEO", VIDEO_URL),
				new PostMediaRequest("IMAGE", IMAGE_URL));

		postRegister.update(USER_ID, post.getId(), new PostUpdateRequest("본문", null, media));
		flushAndClear();

		Post found = postRepository.findById(post.getId()).orElseThrow();
		assertThat(found.getMedia())
				.extracting(PostMedia::getType, PostMedia::getSortOrder)
				.containsExactly(
						tuple(PostMediaType.VIDEO, 0),
						tuple(PostMediaType.IMAGE, 1));
	}

	@Test
	void 다른_사용자의_게시글을_수정하면_예외를_던진다() {
		Post post = savePost(7L, null, List.of());

		assertThatThrownBy(() -> postRegister.update(USER_ID, post.getId(), new PostUpdateRequest("본문", null, null)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void 없는_게시글을_수정하면_예외를_던진다() {
		assertThatThrownBy(() -> postRegister.update(USER_ID, 999_999L, new PostUpdateRequest("본문", null, null)))
				.isInstanceOf(PostNotFoundException.class);
	}

	@Test
	void 수정할_본문이_비어_있으면_예외를_던진다() {
		Post post = savePost(USER_ID, null, List.of());

		assertThatThrownBy(() -> postRegister.update(USER_ID, post.getId(), new PostUpdateRequest(" ", null, null)))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 수정할_때_멘션한_레슨이_없으면_예외를_던진다() {
		Post post = savePost(USER_ID, null, List.of());
		PostUpdateRequest request = new PostUpdateRequest("본문", new MentionRequest("LESSON", 999_999L), null);

		assertThatThrownBy(() -> postRegister.update(USER_ID, post.getId(), request))
				.isInstanceOf(MentionTargetNotFoundException.class);
	}

	@Test
	void 수정할_때_다른_사용자가_올린_미디어면_예외를_던진다() {
		Post post = savePost(USER_ID, null, List.of());
		PostUpdateRequest request = new PostUpdateRequest("본문", null,
				List.of(new PostMediaRequest("IMAGE", "https://images.test.meongcoach.com/images/community-post/7/a.jpg")));

		assertThatThrownBy(() -> postRegister.update(USER_ID, post.getId(), request))
				.isInstanceOf(InvalidMediaUrlException.class);
	}

	@Test
	void 수정할_미디어에_url이_없으면_예외를_던진다() {
		Post post = savePost(USER_ID, null, List.of());
		PostUpdateRequest request = new PostUpdateRequest("본문", null, List.of(new PostMediaRequest("IMAGE", " ")));

		assertThatThrownBy(() -> postRegister.update(USER_ID, post.getId(), request))
				.isInstanceOf(ConstraintViolationException.class);
	}

	private Post savePost(Long userId, MentionCommand mention, List<PostMediaCommand> media) {
		return postRepository.save(Post.register(userId, new PostRegisterCommand("원래 본문", mention, media)));
	}

	private Lesson saveLesson(String title) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create("기본 카테고리", 1, null, null));
		Topic topic = topicRepository.save(TopicFixture.create(category, "기본 토픽", 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(CurriculumFixture.create(topic, "기본 커리큘럼", 1, null, null));
		return lessonRepository.save(LessonFixture.create(curriculum, title, 1, 5));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
