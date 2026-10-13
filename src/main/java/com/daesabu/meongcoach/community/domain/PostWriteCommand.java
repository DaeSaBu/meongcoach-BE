package com.daesabu.meongcoach.community.domain;

public record PostWriteCommand(Long userId, String content, MentionCommand mention) {

	public boolean hasMention() {
		return mention != null;
	}
}
