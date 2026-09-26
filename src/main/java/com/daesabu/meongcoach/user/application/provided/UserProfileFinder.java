package com.daesabu.meongcoach.user.application.provided;

import java.util.Map;
import java.util.Set;

public interface UserProfileFinder {

	Map<Long, String> findNicknames(Set<Long> userIds);
}
