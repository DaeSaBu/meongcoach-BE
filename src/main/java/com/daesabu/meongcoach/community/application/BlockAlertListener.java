package com.daesabu.meongcoach.community.application;

import com.daesabu.meongcoach.alert.application.provided.Alerter;
import com.daesabu.meongcoach.alert.application.provided.dto.AlertRequest;
import com.daesabu.meongcoach.alert.domain.shared.AlertField;
import com.daesabu.meongcoach.community.domain.UserBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BlockAlertListener {

	private final Alerter alerter;

	@TransactionalEventListener
	public void alert(UserBlockedEvent event) {
		alerter.alert(AlertRequest.of("사용자 차단",
				AlertField.of("blockId", event.blockId()),
				AlertField.of("blockerId", event.blockerId()),
				AlertField.of("blockedId", event.blockedId())));
	}
}
