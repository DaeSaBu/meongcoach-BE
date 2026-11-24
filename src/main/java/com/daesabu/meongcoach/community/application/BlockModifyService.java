package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.community.application.provided.BlockRegister;
import com.daesabu.meongcoach.community.application.required.UserBlockRepository;
import com.daesabu.meongcoach.community.domain.UserBlock;
import com.daesabu.meongcoach.community.domain.UserBlockedEvent;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockModifyService implements BlockRegister {

	private final UserBlockRepository userBlockRepository;
	private final UserFinder userFinder;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void block(Long userId, Long targetUserId) {
		UserBlock block = UserBlock.register(userId, targetUserId);
		userFinder.findById(targetUserId);
		userBlockRepository.lockByBlockerId(userId);
		if (userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId)) {
			return;
		}

		userBlockRepository.save(block);
		eventPublisher.publishEvent(UserBlockedEvent.from(block));
	}

	@Override
	@Transactional
	public void unblock(Long userId, Long targetUserId) {
		userBlockRepository.deleteByBlockerIdAndBlockedId(userId, targetUserId);
	}
}
