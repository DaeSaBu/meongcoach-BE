package com.daesabu.meongcoach.community.domain;

public record UserBlockedEvent(Long blockId, Long blockerId, Long blockedId) {

	public static UserBlockedEvent from(UserBlock block) {
		return new UserBlockedEvent(block.getId(), block.getBlockerId(), block.getBlockedId());
	}
}
