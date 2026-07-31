package com.daesabu.meongcoach.media.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import com.daesabu.meongcoach.media.domain.ImageType;
import com.daesabu.meongcoach.media.domain.ImageUploadTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("이미지 객체 키 값 객체")
class ImageObjectKeyTest {

	@Test
	@DisplayName("사용자 프로필 이미지의 객체 키는 대상·사용자 ID·확장자를 담는다")
	void createBuildsUserProfileKey() {
		ImageObjectKey key = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);

		assertThat(key.value()).matches("images/user-profile/7/[0-9a-f-]{36}\\.jpg");
	}

	@Test
	@DisplayName("강아지 프로필 이미지의 객체 키는 강아지 경로 구획을 쓴다")
	void createBuildsDogProfileKey() {
		ImageObjectKey key = ImageObjectKey.create(ImageUploadTarget.DOG_PROFILE, 7L, ImageType.PNG);

		assertThat(key.value()).matches("images/dog-profile/7/[0-9a-f-]{36}\\.png");
	}

	@Test
	@DisplayName("생성할 때마다 서로 다른 객체 키를 만든다")
	void createGeneratesUniqueKeys() {
		ImageObjectKey first = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);
		ImageObjectKey second = ImageObjectKey.create(ImageUploadTarget.USER_PROFILE, 7L, ImageType.JPEG);

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	@DisplayName("같은 값끼리는 동등하다")
	void sameValuesAreEqual() {
		assertThat(new ImageObjectKey("images/user-profile/7/key.jpg"))
				.isEqualTo(new ImageObjectKey("images/user-profile/7/key.jpg"));
	}
}
