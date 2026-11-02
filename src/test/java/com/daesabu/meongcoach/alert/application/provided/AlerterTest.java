package com.daesabu.meongcoach.alert.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import com.daesabu.meongcoach.alert.application.required.AlertSender;
import com.daesabu.meongcoach.alert.domain.Alert;
import com.daesabu.meongcoach.alert.domain.exception.AlertDeliveryFailedException;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.alert.domain.shared.AlertLevel;
import com.daesabu.meongcoach.support.ApplicationTest;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

@ApplicationTest
class AlerterTest {

	private static final long ASYNC_TIMEOUT_MILLIS = 2000;
	private static final long ASYNC_SETTLE_MILLIS = 300;

	@Autowired
	private Alerter alerter;

	@Autowired
	private AlertSender alertSender;

	@Test
	void 알림을_비동기로_전송기에_전달한다() {
		Instant before = Instant.now();
		AlertRequest request = AlertRequest.of(AlertLevel.INFO, "신규 가입",
				AlertField.of("userId", 42L), AlertField.of("provider", "KAKAO"));

		alerter.alert(request);

		ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(captor.capture());
		Alert alert = captor.getValue();
		assertThat(alert.level()).isEqualTo(AlertLevel.INFO);
		assertThat(alert.title()).isEqualTo("신규 가입");
		assertThat(alert.fields()).containsExactly(new AlertField("userId", "42"), new AlertField("provider", "KAKAO"));
		assertThat(alert.occurredAt()).isAfterOrEqualTo(before);
	}

	@Test
	void 전송기가_실패해도_호출자에게_예외가_전파되지_않는다() {
		willThrow(new AlertDeliveryFailedException("status=500")).given(alertSender).send(any());
		AlertRequest request = AlertRequest.of(AlertLevel.ERROR, "영상 분석 실패", AlertField.of("reportId", 7L));

		assertThatCode(() -> alerter.alert(request)).doesNotThrowAnyException();

		verify(alertSender, timeout(ASYNC_TIMEOUT_MILLIS)).send(any());
	}

	@Test
	void 제목이_비어_있으면_예외_없이_끝나고_전송하지_않는다() {
		AlertRequest request = new AlertRequest(AlertLevel.INFO, " ", List.of());

		assertThatCode(() -> alerter.alert(request)).doesNotThrowAnyException();

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 필드_이름이_비어_있으면_전송하지_않는다() {
		AlertRequest request = new AlertRequest(AlertLevel.INFO, "신규 가입", List.of(new AlertField(" ", "42")));

		assertThatCode(() -> alerter.alert(request)).doesNotThrowAnyException();

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 필드가_20개를_넘으면_전송하지_않는다() {
		List<AlertField> fields = IntStream.range(0, 21)
				.mapToObj(index -> AlertField.of("field" + index, index))
				.toList();
		AlertRequest request = new AlertRequest(AlertLevel.INFO, "신규 가입", fields);

		assertThatCode(() -> alerter.alert(request)).doesNotThrowAnyException();

		verify(alertSender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}
}
