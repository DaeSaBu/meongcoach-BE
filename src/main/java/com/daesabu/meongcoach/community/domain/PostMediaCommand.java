package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

public record PostMediaCommand(PostMediaType type, String url) {

	public PostMediaCommand {
		requireNonNull(type);
		requireNonNull(url);
	}
}
