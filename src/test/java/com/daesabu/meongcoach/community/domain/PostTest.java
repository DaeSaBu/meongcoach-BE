package com.daesabu.meongcoach.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.daesabu.meongcoach.community.domain.exception.InvalidPostContentException;
import com.daesabu.meongcoach.community.domain.exception.PostMediaCountExceededException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PostTest {

	private static final Long USER_ID = 42L;
	private static final String CONTENT = "오늘 앉아 훈련 성공!";
	private static final String IMAGE_URL = "https://media.meongcoach.com/images/community/42/photo.jpg";
	private static final String VIDEO_URL = "https://media.meongcoach.com/videos/community/42/clip.mp4";

	@Test
	void 작성하면_작성자와_본문이_채워진다() {
		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, List.of()));

		assertThat(post.getUserId()).isEqualTo(USER_ID);
		assertThat(post.getContent()).isEqualTo(new PostContent(CONTENT));
	}

	@Test
	void 멘션과_함께_작성하면_멘션이_채워진다() {
		MentionCommand mention = new MentionCommand(MentionTargetType.LESSON, 10L);

		Post post = Post.register(USER_ID, new PostRegisterCommand("이 레슨 따라해봤어요", mention, List.of()));

		assertThat(post.getMention().getTargetType()).isEqualTo(MentionTargetType.LESSON);
		assertThat(post.getMention().getTargetId()).isEqualTo(10L);
	}

	@Test
	void 본문이_비어_있으면_작성할_수_없다() {
		assertThatThrownBy(() -> Post.register(USER_ID, new PostRegisterCommand("   ", null, List.of())))
				.isInstanceOf(InvalidPostContentException.class);
	}

	@Test
	void 미디어와_함께_작성하면_첨부_순서대로_채워진다() {
		List<PostMediaCommand> media = List.of(
				new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL),
				new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL));

		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, media));

		assertThat(post.getMedia())
				.extracting(PostMedia::getType, PostMedia::getUrl, PostMedia::getSortOrder)
				.containsExactly(
						tuple(PostMediaType.VIDEO, VIDEO_URL, 0),
						tuple(PostMediaType.IMAGE, IMAGE_URL, 1));
	}

	@Test
	void 미디어_없이_작성하면_미디어가_비어_있다() {
		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, null));

		assertThat(post.getMedia()).isEmpty();
	}

	@Test
	void 사진은_3개까지_첨부할_수_있다() {
		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, images(3)));

		assertThat(post.getMedia()).hasSize(3);
	}

	@Test
	void 사진이_3개를_넘으면_작성할_수_없다() {
		assertThatThrownBy(() -> Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, images(4))))
				.isInstanceOf(PostMediaCountExceededException.class);
	}

	@Test
	void 영상은_2개까지_첨부할_수_있다() {
		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, videos(2)));

		assertThat(post.getMedia()).hasSize(2);
	}

	@Test
	void 영상이_2개를_넘으면_작성할_수_없다() {
		assertThatThrownBy(() -> Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, videos(3))))
				.isInstanceOf(PostMediaCountExceededException.class);
	}

	@Test
	void 사진_3개와_영상_2개를_함께_첨부할_수_있다() {
		List<PostMediaCommand> media = Stream.concat(images(3).stream(), videos(2).stream()).toList();

		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, media));

		assertThat(post.getMedia()).hasSize(5);
	}

	@Test
	void 미디어_목록은_밖에서_바꿀_수_없다() {
		Post post = Post.register(USER_ID, new PostRegisterCommand(CONTENT, null, images(2)));

		assertThatThrownBy(() -> post.getMedia().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	private List<PostMediaCommand> images(int count) {
		return Collections.nCopies(count, new PostMediaCommand(PostMediaType.IMAGE, IMAGE_URL));
	}

	private List<PostMediaCommand> videos(int count) {
		return Collections.nCopies(count, new PostMediaCommand(PostMediaType.VIDEO, VIDEO_URL));
	}
}
