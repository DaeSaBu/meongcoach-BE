package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.domain.MentionTargetType;
import com.daesabu.meongcoach.community.domain.PostMention;
import com.daesabu.meongcoach.community.domain.exception.MentionTargetNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class MentionTargetVerifiers {

	private final Map<MentionTargetType, MentionTargetVerifier> verifiers = new EnumMap<>(MentionTargetType.class);

	public MentionTargetVerifiers(List<MentionTargetVerifier> verifiers) {
		verifiers.forEach(verifier -> this.verifiers.put(verifier.targetType(), verifier));
		validateAllTargetTypesSupported();
	}

	public void validateExists(PostMention mention) {
		MentionTargetVerifier verifier = verifiers.get(mention.getTargetType());
		if (!verifier.exists(mention.getTargetId())) {
			throw new MentionTargetNotFoundException(mention.getTargetId());
		}
	}

	private void validateAllTargetTypesSupported() {
		List<MentionTargetType> unsupported = Stream.of(MentionTargetType.values())
				.filter(targetType -> !verifiers.containsKey(targetType))
				.toList();
		if (unsupported.isEmpty()) {
			return;
		}

		throw new IllegalStateException("확인기가 없는 멘션 대상 종류가 있습니다: " + unsupported);
	}
}
