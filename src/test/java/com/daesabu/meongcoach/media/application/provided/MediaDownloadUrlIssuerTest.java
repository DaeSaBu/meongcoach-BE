package com.daesabu.meongcoach.media.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daesabu.meongcoach.media.domain.exception.InvalidMediaObjectKeyException;
import com.daesabu.meongcoach.media.domain.shared.MediaKind;
import com.daesabu.meongcoach.media.domain.shared.MediaScope;
import com.daesabu.meongcoach.media.domain.shared.MediaVisibility;
import com.daesabu.meongcoach.support.ApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class MediaDownloadUrlIssuerTest {

	private static final String S3_BASE_URL = "https://test-video-bucket.s3.ap-northeast-2.amazonaws.com/";
	private static final MediaScope GRADUATION_VIDEO =
			MediaScope.of(MediaKind.VIDEO, MediaVisibility.PRIVATE, "graduation");

	@Autowired
	private MediaDownloadUrlIssuer issuer;

	@Test
	void 범위_아래의_객체_키면_비공개_스토리지의_다운로드_URL을_발급한다() {
		String downloadUrl = issuer.issue(GRADUATION_VIDEO, "videos/graduation/7/1101/a.mp4");

		assertThat(downloadUrl)
				.startsWith(S3_BASE_URL + "videos/graduation/7/1101/a.mp4")
				.contains("X-Amz-Signature=")
				.contains("X-Amz-Expires=3600");
	}

	@Test
	void 범위_아래의_객체_키가_아니면_발급에_실패한다() {
		assertThatThrownBy(() -> issuer.issue(GRADUATION_VIDEO, "videos/training/7/a.mp4"))
				.isInstanceOf(InvalidMediaObjectKeyException.class);
	}
}
