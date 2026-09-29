package com.daesabu.meongcoach.entitlement.adapter.webapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RevenueCat 웹훅 본문 중 동기화할 회원을 고르는 데 쓰는 부분만 담는다. 이벤트 내용은 해석하지 않고 RevenueCat REST API로 최신 상태를 다시 읽는다.
 * 이벤트 타입마다 들어오는 필드가 달라(TRANSFER에는 app_user_id 대신 transferred_from·transferred_to가 온다) type만 제약을 둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(@Valid @NotNull Event event) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Event(
			@NotBlank String type,
			@JsonProperty("app_user_id") String appUserId,
			@JsonProperty("transferred_from") List<String> transferredFrom,
			@JsonProperty("transferred_to") List<String> transferredTo
	) {

		// 회원 ID는 Long 범위 안의 숫자다. 익명 ID($RCAnonymousID:…)처럼 우리 회원이 아닌 RevenueCat 고객을 걸러 낸다
		private static final Pattern USER_ID = Pattern.compile("\\d{1,18}");

		/**
		 * 이벤트와 관련된 회원 ID. 계정 이전이면 이용권을 잃는 회원과 받는 회원이 모두 들어 있다.
		 */
		public Set<Long> userIds() {
			return Stream.of(Stream.ofNullable(appUserId), streamOf(transferredFrom), streamOf(transferredTo))
					.flatMap(ids -> ids)
					.filter(Objects::nonNull)
					.filter(id -> USER_ID.matcher(id).matches())
					.map(Long::valueOf)
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		private static Stream<String> streamOf(List<String> ids) {
			if (ids == null) {
				return Stream.empty();
			}
			return ids.stream();
		}
	}
}
