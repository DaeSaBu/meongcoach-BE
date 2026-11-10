package com.daesabu.meongcoach.support.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.support.domain.exception.ChannelTalkUnavailableException;
import org.junit.jupiter.api.Test;

class ChannelTalkPropertiesTest {
	private static final String KEY = "4629de5def93d6a2abea6afa9bd5476d9c6cbc04223f9a2f7e517b535dde3e25";

	@Test
	void 회원_해시는_공식_예제와_같은_바이트_키로_생성한다() {
		var properties = new ChannelTalkProperties("token", KEY);
		assertThat(properties.memberHash("lucas")).isEqualTo("99427c7bba36a6902c5fd6383f2fb0214d19b81023296b4bd6b9e024836afea2");
	}

	@Test
	void 토큰이_없거나_다르면_거절한다() {
		var properties = new ChannelTalkProperties("token", KEY);
		assertThat(properties.matches("token")).isTrue();
		assertThat(properties.matches(null)).isFalse();
		assertThat(properties.matches("wrong")).isFalse();
	}

	@Test
	void 해시_키가_없으면_익명_상담으로_대체하지_않는다() {
		var properties = new ChannelTalkProperties("token", "");
		assertThatThrownBy(() -> properties.memberHash("42")).isInstanceOf(ChannelTalkUnavailableException.class);
	}
}
