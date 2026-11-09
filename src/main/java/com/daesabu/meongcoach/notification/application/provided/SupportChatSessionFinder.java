package com.daesabu.meongcoach.notification.application.provided;

import com.daesabu.meongcoach.notification.application.provided.dto.SupportChatSessionResult;

public interface SupportChatSessionFinder {
	SupportChatSessionResult find(Long userId);
}
