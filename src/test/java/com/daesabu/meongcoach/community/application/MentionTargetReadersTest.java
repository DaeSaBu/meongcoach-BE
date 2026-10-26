package com.daesabu.meongcoach.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionCommand;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MentionTargetReadersTest {

	private static final MentionTargetReaders READERS = new MentionTargetReaders(
			List.of(new StubMentionTargetReader(MentionTargetType.LESSON, Map.of(1L, "앉아 기초", 2L, "기다려"))));

	@Test
	void 대상이_있으면_존재_확인을_통과한다() {
		PostMention mention = mentionOf(MentionTargetType.LESSON, 1L);

		assertThatCode(() -> READERS.validateExists(mention))
				.doesNotThrowAnyException();
	}

	@Test
	void 대상이_없으면_예외를_던진다() {
		PostMention mention = mentionOf(MentionTargetType.LESSON, 999L);

		assertThatThrownBy(() -> READERS.validateExists(mention))
				.isInstanceOf(MentionTargetNotFoundException.class);
	}

	@Test
	void 멘션을_대상_종류별로_묶어_요약을_조회한다() {
		List<PostMention> mentions = List.of(
				mentionOf(MentionTargetType.LESSON, 1L),
				mentionOf(MentionTargetType.LESSON, 2L),
				mentionOf(MentionTargetType.LESSON, 1L));

		Map<MentionTargetType, Map<Long, MentionTargetResult>> summaries = READERS.findSummaries(mentions);

		assertThat(summaries.get(MentionTargetType.LESSON)).containsOnly(
				Map.entry(1L, new MentionTargetResult(MentionTargetType.LESSON, 1L, "앉아 기초")),
				Map.entry(2L, new MentionTargetResult(MentionTargetType.LESSON, 2L, "기다려")));
	}

	@Test
	void 없는_대상은_요약에서_빠진다() {
		List<PostMention> mentions = List.of(
				mentionOf(MentionTargetType.LESSON, 1L),
				mentionOf(MentionTargetType.LESSON, 999L));

		Map<MentionTargetType, Map<Long, MentionTargetResult>> summaries = READERS.findSummaries(mentions);

		assertThat(summaries.get(MentionTargetType.LESSON)).containsOnlyKeys(1L);
	}

	@Test
	void 리더가_없는_대상_종류가_있으면_생성할_수_없다() {
		List<MentionTargetReader> readers = List.of();

		assertThatThrownBy(() -> new MentionTargetReaders(readers))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("LESSON");
	}

	private static PostMention mentionOf(MentionTargetType targetType, Long targetId) {
		MentionCommand mention = new MentionCommand(targetType, targetId);
		Post post = Post.register(42L, new PostRegisterCommand("본문", mention, List.of()));
		return post.getMention();
	}

	private record StubMentionTargetReader(MentionTargetType targetType, Map<Long, String> titles)
			implements MentionTargetReader {

		@Override
		public List<MentionTargetResult> findSummaries(Collection<Long> targetIds) {
			return targetIds.stream()
					.filter(titles::containsKey)
					.map(targetId -> new MentionTargetResult(targetType, targetId, titles.get(targetId)))
					.toList();
		}
	}
}
