package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostListRequest;
import com.daesabu.meongcoach.community.application.provided.dto.PostListResult;
import com.daesabu.meongcoach.community.application.provided.dto.PostResult;
import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostContent;
import com.daesabu.meongcoach.community.domain.PostWriteCommand;
import com.daesabu.meongcoach.community.domain.exception.PostNotFoundException;
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
import com.daesabu.meongcoach.user.application.provided.UserProfileSummaryResult;
import com.daesabu.meongcoach.user.application.required.UserProfileRepository;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfile;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class PostFinderTest {

	private static final Long USER_ID = 42L;
	private static final Long LESSON_A_ID = 101L;
	private static final Long LESSON_B_ID = 202L;

	@Autowired
	private PostFinder postFinder;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserProfileRepository userProfileRepository;

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
	void 게시글을_최신순으로_반환한다() {
		savePost(USER_ID, "첫 번째");
		savePost(USER_ID, "두 번째");
		savePost(USER_ID, "세 번째");
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts()).extracting(this::contentOf).containsExactly("세 번째", "두 번째", "첫 번째");
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void size보다_게시글이_많으면_다음_커서와_함께_size개만_반환한다() {
		List<Post> posts = savePosts(3);
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 2, null));

		assertThat(result.posts()).extracting(this::contentOf).containsExactly("글 3", "글 2");
		assertThat(result.nextCursor()).isEqualTo(posts.get(1).getId());
	}

	@Test
	void 커서를_넘기면_그보다_작은_id의_게시글만_반환한다() {
		List<Post> posts = savePosts(3);
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(posts.get(1).getId(), 2, null));

		assertThat(result.posts()).extracting(this::contentOf).containsExactly("글 1");
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void 다음_커서로_이어서_조회하면_중복이나_누락_없이_전체를_순회한다() {
		savePosts(5);
		flushAndClear();

		PostListResult first = postFinder.findPosts(new PostListRequest(null, 2, null));
		PostListResult second = postFinder.findPosts(new PostListRequest(first.nextCursor(), 2, null));
		PostListResult third = postFinder.findPosts(new PostListRequest(second.nextCursor(), 2, null));

		assertThat(first.posts()).extracting(this::contentOf).containsExactly("글 5", "글 4");
		assertThat(second.posts()).extracting(this::contentOf).containsExactly("글 3", "글 2");
		assertThat(third.posts()).extracting(this::contentOf).containsExactly("글 1");
		assertThat(third.nextCursor()).isNull();
	}

	@Test
	void size를_생략하면_20개를_반환한다() {
		savePosts(21);
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, null, null));

		assertThat(result.posts()).hasSize(20);
		assertThat(result.nextCursor()).isNotNull();
	}

	@Test
	void size가_1_미만이면_예외를_던진다() {
		assertThatThrownBy(() -> postFinder.findPosts(new PostListRequest(null, 0, null)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void size가_50을_넘으면_예외를_던진다() {
		assertThatThrownBy(() -> postFinder.findPosts(new PostListRequest(null, 51, null)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void cursor가_양수가_아니면_예외를_던진다() {
		assertThatThrownBy(() -> postFinder.findPosts(new PostListRequest(0L, 20, null)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void lessonId가_양수가_아니면_예외를_던진다() {
		assertThatThrownBy(() -> postFinder.findPosts(new PostListRequest(null, 20, 0L)))
				.isInstanceOf(ConstraintViolationException.class);
	}

	@Test
	void 레슨을_넘기면_그_레슨을_멘션한_게시글만_최신순으로_반환한다() {
		savePost(USER_ID, "레슨 A 첫 글", LESSON_A_ID);
		savePost(USER_ID, "레슨 B 글", LESSON_B_ID);
		savePost(USER_ID, "멘션 없는 글");
		savePost(USER_ID, "레슨 A 둘째 글", LESSON_A_ID);
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, LESSON_A_ID));

		assertThat(result.posts()).extracting(this::contentOf).containsExactly("레슨 A 둘째 글", "레슨 A 첫 글");
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void 레슨과_커서로_이어서_조회하면_중복이나_누락_없이_해당_레슨_게시글을_순회한다() {
		IntStream.rangeClosed(1, 3).forEach(index -> {
			savePost(USER_ID, "레슨 A 글 " + index, LESSON_A_ID);
			savePost(USER_ID, "레슨 B 글 " + index, LESSON_B_ID);
		});
		flushAndClear();

		PostListResult first = postFinder.findPosts(new PostListRequest(null, 2, LESSON_A_ID));
		PostListResult second = postFinder.findPosts(new PostListRequest(first.nextCursor(), 2, LESSON_A_ID));

		assertThat(first.posts()).extracting(this::contentOf).containsExactly("레슨 A 글 3", "레슨 A 글 2");
		assertThat(second.posts()).extracting(this::contentOf).containsExactly("레슨 A 글 1");
		assertThat(second.nextCursor()).isNull();
	}

	@Test
	void 작성자_프로필_요약을_함께_반환한다() {
		User author = saveUserWithProfile("멍멍이집사", "https://cdn.example.com/profile.png");
		savePost(author.getId(), "본문");
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts().getFirst().author()).isEqualTo(
				new UserProfileSummaryResult(author.getId(), "멍멍이집사", "https://cdn.example.com/profile.png"));
	}

	@Test
	void 프로필이_없는_작성자의_게시글은_author가_null이다() {
		savePost(USER_ID, "본문");
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts().getFirst().author()).isNull();
	}

	@Test
	void 멘션_대상_요약을_함께_반환한다() {
		Lesson lesson = saveLesson("기본 교육");
		savePost(USER_ID, "본문", lesson.getId());
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts()).extracting(PostResult::mentionTarget)
				.extracting(MentionTargetResult::targetType, MentionTargetResult::targetId, MentionTargetResult::title)
				.containsExactly(tuple(MentionTargetType.LESSON, lesson.getId(), "기본 교육 레슨"));
	}

	@Test
	void 멘션이_없는_게시글은_mentionTarget이_null이다() {
		savePost(USER_ID, "본문");
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts().getFirst().mentionTarget()).isNull();
	}

	@Test
	void 멘션_대상이_없으면_mentionTarget이_null이다() {
		savePost(USER_ID, "본문", -1L);
		flushAndClear();

		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts().getFirst().mentionTarget()).isNull();
	}

	@Test
	void 게시글이_없으면_빈_목록을_반환한다() {
		PostListResult result = postFinder.findPosts(new PostListRequest(null, 20, null));

		assertThat(result.posts()).isEmpty();
		assertThat(result.nextCursor()).isNull();
	}

	@Test
	void 게시글_하나를_id로_조회한다() {
		Post saved = savePost(USER_ID, "본문");
		flushAndClear();

		Post found = postFinder.findPost(saved.getId());

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getContent().value()).isEqualTo("본문");
	}

	@Test
	void 조회한_게시글이_없으면_예외를_던진다() {
		assertThatThrownBy(() -> postFinder.findPost(999_999L))
				.isInstanceOf(PostNotFoundException.class);
	}

	private String contentOf(PostResult result) {
		PostContent content = result.post().getContent();
		return content.value();
	}

	private List<Post> savePosts(int count) {
		return IntStream.rangeClosed(1, count)
				.mapToObj(index -> savePost(USER_ID, "글 " + index))
				.toList();
	}

	private Post savePost(Long userId, String content) {
		return postRepository.save(Post.register(userId, new PostWriteCommand(content, null, List.of())));
	}

	private Post savePost(Long userId, String content, Long lessonId) {
		MentionCommand mention = new MentionCommand(MentionTargetType.LESSON, lessonId);
		return postRepository.save(Post.register(userId, new PostWriteCommand(content, mention, List.of())));
	}

	private User saveUserWithProfile(String nickname, String profileImageUrl) {
		User user = userRepository.save(User.registerUser());
		userProfileRepository.save(UserProfile.create(user,
				new UserProfileCreateCommand(nickname, profileImageUrl, "INFP", Set.of(), Set.of())));
		return user;
	}

	private Lesson saveLesson(String title) {
		TrainingCategory category = trainingCategoryRepository.save(
				TrainingCategoryFixture.create(title + " 카테고리", 1, null, null));
		Topic topic = topicRepository.save(TopicFixture.create(category, title, 1, null, null, null));
		Curriculum curriculum = curriculumRepository.save(
				CurriculumFixture.create(topic, title + " 커리큘럼", 1, null, null));
		return lessonRepository.save(LessonFixture.create(curriculum, title + " 레슨", 1, 5));
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
