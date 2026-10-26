package com.daesabu.meongcoach.community.domain;

import java.util.List;

public record PostRegisterCommand(String content, MentionCommand mention, List<PostMediaCommand> media) {
}
