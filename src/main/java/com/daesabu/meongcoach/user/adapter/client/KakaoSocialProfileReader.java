package com.daesabu.meongcoach.user.adapter.client;

import com.daesabu.meongcoach.user.adapter.client.dto.KakaoAccessTokenInfoResponse;
import com.daesabu.meongcoach.user.adapter.client.dto.KakaoUserInfoResponse;
import com.daesabu.meongcoach.user.application.required.SocialProfileReader;
import com.daesabu.meongcoach.user.domain.SocialAccountLinkCommand;
import com.daesabu.meongcoach.user.domain.SocialProvider;
import com.daesabu.meongcoach.user.domain.exception.InvalidSocialTokenException;
import com.daesabu.meongcoach.user.domain.exception.SocialProviderUnavailableException;
import com.daesabu.meongcoach.user.domain.exception.SocialTokenAppMismatchException;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 앱이 카카오 SDK로 받은 액세스 토큰을 서버가 검증한다.
 * 토큰 자체는 로그인 시점에만 쓰고 저장하지 않으며, 이후 인가는 우리 JWT로만 한다.
 */
@Component
public class KakaoSocialProfileReader implements SocialProfileReader {

	private static final String TOKEN_INFO_PATH = "/v1/user/access_token_info";
	private static final String USER_INFO_PATH = "/v2/user/me";
	private static final String BEARER_PREFIX = "Bearer ";

	private final RestClient restClient;
	private final KakaoProperties properties;

	public KakaoSocialProfileReader(RestClient.Builder builder, KakaoProperties properties) {
		this.restClient = builder.baseUrl(properties.apiBaseUrl()).build();
		this.properties = properties;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.KAKAO;
	}

	@Override
	public SocialAccountLinkCommand read(String credential) {
		KakaoAccessTokenInfoResponse tokenInfo = get(TOKEN_INFO_PATH, credential, KakaoAccessTokenInfoResponse.class);
		// 다른 앱에서 발급된 토큰이면 해당 카카오 사용자로 로그인할 수 있으므로 반드시 대조한다
		if (!Objects.equals(tokenInfo.appId(), properties.appId())) {
			throw new SocialTokenAppMismatchException();
		}

		KakaoUserInfoResponse userInfo = get(USER_INFO_PATH, credential, KakaoUserInfoResponse.class);

		return new SocialAccountLinkCommand(SocialProvider.KAKAO, String.valueOf(tokenInfo.id()),
				userInfo.resolveEmail());
	}

	private <T> T get(String path, String credential, Class<T> responseType) {
		try {
			return restClient.get()
					.uri(path)
					.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + credential)
					.retrieve()
					.onStatus(status -> status.value() == 401, (request, response) -> {
						throw new InvalidSocialTokenException();
					})
					.body(responseType);
		} catch (RestClientException e) {
			// 카카오 장애·타임아웃은 토큰 무효와 구분해야 클라이언트가 재시도할 수 있다
			throw new SocialProviderUnavailableException();
		}
	}
}
