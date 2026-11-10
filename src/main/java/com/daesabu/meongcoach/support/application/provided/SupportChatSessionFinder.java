package com.daesabu.meongcoach.support.application.provided;

import com.daesabu.meongcoach.support.application.provided.dto.SupportChatSessionResult;

public interface SupportChatSessionFinder {
	SupportChatSessionResult find(Long userId);
}
