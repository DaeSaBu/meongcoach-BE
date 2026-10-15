package com.daesabu.meongcoach.plan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoreanParticleTest {

	@Test
	void 받침이_있으면_을을_쓴다() {
		assertThat(KoreanParticle.objectOf("앉아서 기다려")).isEqualTo("를");
		assertThat(KoreanParticle.objectOf("산책 예절")).isEqualTo("을");
		assertThat(KoreanParticle.objectOf("배변 훈련")).isEqualTo("을");
	}

	@Test
	void 받침이_없으면_를을_쓴다() {
		assertThat(KoreanParticle.objectOf("기초 매너")).isEqualTo("를");
		assertThat(KoreanParticle.objectOf("사회화")).isEqualTo("를");
	}

	@Test
	void 앞뒤_공백은_무시하고_마지막_글자로_판단한다() {
		assertThat(KoreanParticle.objectOf("산책  ")).isEqualTo("을");
	}

	@Test
	void 한글이_아니면_을_또는_를로_쓴다() {
		assertThat(KoreanParticle.objectOf("ABC")).isEqualTo("을(를)");
		assertThat(KoreanParticle.objectOf("3단계")).isEqualTo("를");
		assertThat(KoreanParticle.objectOf("1")).isEqualTo("을(를)");
	}

	@Test
	void 단어가_비어_있으면_을_또는_를로_쓴다() {
		assertThat(KoreanParticle.objectOf("")).isEqualTo("을(를)");
		assertThat(KoreanParticle.objectOf(null)).isEqualTo("을(를)");
	}
}
