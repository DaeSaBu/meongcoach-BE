package com.daesabu.meongcoach.notification.application.provided;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class SupportChatSessionFinderTest {
	@Autowired SupportChatSessionFinder finder;

	@Test
	void 회원_ID_문자열을_그대로_서명한다() {
		var result = finder.find(42L);

		assertThat(result.memberId()).isEqualTo("42");
		assertThat(result.memberHash()).isEqualTo("d850a63a23e8b16a6f9b2e7554f195ec5ee8186afaa10ba895b3eaacce93bf92");
	}
}
