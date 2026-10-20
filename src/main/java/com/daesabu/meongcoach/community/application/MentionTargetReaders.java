package com.daesabu.meongcoach.community.application;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.daesabu.meongcoach.community.application.provided.dto.MentionTargetResult;
import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class MentionTargetReaders {

	private final Map<MentionTargetType, MentionTargetReader> readers = new EnumMap<>(MentionTargetType.class);

	public MentionTargetReaders(List<MentionTargetReader> readers) {
		readers.forEach(reader -> this.readers.put(reader.targetType(), reader));
		validateAllTargetTypesSupported();
	}

	public void validateExists(PostMention mention) {
		MentionTargetReader reader = readers.get(mention.getTargetType());
		List<MentionTargetResult> found = reader.findSummaries(List.of(mention.getTargetId()));
		if (found.isEmpty()) {
			throw new MentionTargetNotFoundException(mention.getTargetId());
		}
	}

	public Map<MentionTargetType, Map<Long, MentionTargetResult>> findSummaries(Collection<PostMention> mentions) {
		Map<MentionTargetType, Set<Long>> targetIds = mentions.stream()
				.collect(groupingBy(PostMention::getTargetType, () -> new EnumMap<>(MentionTargetType.class),
						mapping(PostMention::getTargetId, toSet())));

		Map<MentionTargetType, Map<Long, MentionTargetResult>> summaries = new EnumMap<>(MentionTargetType.class);
		targetIds.forEach((targetType, ids) -> summaries.put(targetType, findSummariesById(targetType, ids)));
		return summaries;
	}

	private Map<Long, MentionTargetResult> findSummariesById(MentionTargetType targetType, Set<Long> targetIds) {
		MentionTargetReader reader = readers.get(targetType);
		return reader.findSummaries(targetIds).stream()
				.collect(toMap(MentionTargetResult::targetId, Function.identity()));
	}

	private void validateAllTargetTypesSupported() {
		List<MentionTargetType> unsupported = Stream.of(MentionTargetType.values())
				.filter(targetType -> !readers.containsKey(targetType))
				.toList();
		if (unsupported.isEmpty()) {
			return;
		}

		throw new IllegalStateException("리더가 없는 멘션 대상 종류가 있습니다: " + unsupported);
	}
}
