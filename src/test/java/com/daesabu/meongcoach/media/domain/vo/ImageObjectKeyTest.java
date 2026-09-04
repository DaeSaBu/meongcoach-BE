package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import org.junit.jupiter.api.Test;

class ImageObjectKeyTest {

	@Test
	void 사용자_프로필_이미지의_객체_키는_대상_사용자_ID_확장자를_담는다() {
		ImageObjectKey key = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);

		assertThat(key.value()).matches("images/user-profile/7/[0-9a-f-]{36}\\.jpg");
	}

	@Test
	void 강아지_프로필_이미지의_객체_키는_강아지_경로_구획을_쓴다() {
		ImageObjectKey key = ImageObjectKey.create(ImageUploadTarget.DOG_PROFILE, 7L, ImageType.PNG);

		assertThat(key.value()).matches("images/dog-profile/7/[0-9a-f-]{36}\\.png");
	}

	@Test
	void 생성할_때마다_서로_다른_객체_키를_만든다() {
		ImageObjectKey first = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);
		ImageObjectKey second = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void 같은_값끼리는_동등하다() {
		assertThat(new ImageObjectKey("images/user-profile/7/key.jpg"))
				.isEqualTo(new ImageObjectKey("images/user-profile/7/key.jpg"));
	}
}
