package com.daesabu.meongcoach.user.adapter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("카카오 소셜 토큰 검증 클라이언트")
class KakaoSocialProfileReaderTest {

	private static final String BASE_URL = "https://kapi.kakao.com";
	private static final String TOKEN_INFO_URL = BASE_URL + "/v1/user/access_token_info";
	private static final String USER_INFO_URL = BASE_URL + "/v2/user/me";
	private static final String CREDENTIAL = "kakao-access-token";
	private static final Long APP_ID = 1234567L;

	private MockRestServiceServer server;
	private KakaoSocialProfileReader reader;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		reader = new KakaoSocialProfileReader(builder, new KakaoProperties(BASE_URL, APP_ID));
	}

	@Test
	@DisplayName("담당 제공자는 카카오다")
	void providerIsKakao() {
		assertThat(reader.provider()).isEqualTo(SocialProvider.KAKAO);
	}

	@Test
	@DisplayName("유효한 토큰이면 회원 식별자와 이메일을 읽는다")
	void readReturnsProviderIdAndEmail() {
		expectTokenInfo("{\"id\":3812345678,\"app_id\":1234567}");
		expectUserInfo("{\"id\":3812345678,\"kakao_account\":{\"email\":\"a@b.com\"}}");

		SocialAccountLinkCommand command = reader.read(CREDENTIAL);

		assertThat(command.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(command.providerId()).isEqualTo("3812345678");
		assertThat(command.email()).isEqualTo("a@b.com");
		server.verify();
	}

	@Test
	@DisplayName("이메일 동의를 받지 못하면 이메일 없이 읽는다")
	void readReturnsNullEmailWhenAccountIsAbsent() {
		expectTokenInfo("{\"id\":3812345678,\"app_id\":1234567}");
		expectUserInfo("{\"id\":3812345678}");

		SocialAccountLinkCommand command = reader.read(CREDENTIAL);

		assertThat(command.providerId()).isEqualTo("3812345678");
		assertThat(command.email()).isNull();
	}

	@Test
	@DisplayName("이메일 필드가 비어 있어도 읽기에 성공한다")
	void readReturnsNullEmailWhenEmailIsAbsent() {
		expectTokenInfo("{\"id\":3812345678,\"app_id\":1234567}");
		expectUserInfo("{\"id\":3812345678,\"kakao_account\":{}}");

		assertThat(reader.read(CREDENTIAL).email()).isNull();
	}

	@Test
	@DisplayName("다른 앱에서 발급된 토큰이면 회원 정보를 조회하지 않고 거부한다")
	void readFailsWhenAppIdDoesNotMatch() {
		expectTokenInfo("{\"id\":3812345678,\"app_id\":9999999}");

		assertThatThrownBy(() -> reader.read(CREDENTIAL))
				.isInstanceOf(SocialTokenAppMismatchException.class);
		// 회원 정보 조회 기대를 걸지 않았으므로, 호출됐다면 verify가 실패한다
		server.verify();
	}

	@Test
	@DisplayName("카카오가 토큰을 거부하면 유효하지 않은 토큰으로 처리한다")
	void readFailsWhenKakaoRejectsToken() {
		server.expect(requestTo(TOKEN_INFO_URL)).andRespond(withUnauthorizedRequest());

		assertThatThrownBy(() -> reader.read(CREDENTIAL))
				.isInstanceOf(InvalidSocialTokenException.class);
	}

	@Test
	@DisplayName("카카오 장애는 토큰 무효와 구분해 처리한다")
	void readFailsWhenKakaoIsUnavailable() {
		server.expect(requestTo(TOKEN_INFO_URL)).andRespond(withServerError());

		assertThatThrownBy(() -> reader.read(CREDENTIAL))
				.isInstanceOf(SocialProviderUnavailableException.class);
	}

	private void expectTokenInfo(String body) {
		server.expect(requestTo(TOKEN_INFO_URL))
				.andExpect(header("Authorization", "Bearer " + CREDENTIAL))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}

	private void expectUserInfo(String body) {
		server.expect(requestTo(USER_INFO_URL))
				.andExpect(header("Authorization", "Bearer " + CREDENTIAL))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}
}
