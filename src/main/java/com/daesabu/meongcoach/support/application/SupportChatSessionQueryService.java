package com.daesabu.meongcoach.support.application;

import com.daesabu.meongcoach.support.application.provided.SupportChatSessionFinder;
import com.daesabu.meongcoach.support.application.provided.dto.SupportChatSessionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportChatSessionQueryService implements SupportChatSessionFinder {
	private final ChannelTalkProperties properties;

	@Override
	public SupportChatSessionResult find(Long userId) {
		String memberId = userId.toString();
		String memberHash = properties.memberHash(memberId);
		return new SupportChatSessionResult(memberId, memberHash);
	}
}
