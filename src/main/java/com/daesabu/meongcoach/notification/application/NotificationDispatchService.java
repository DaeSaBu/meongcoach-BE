package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.provided.NotificationDispatchResult;
import com.daesabu.meongcoach.notification.application.provided.NotificationDispatcher;
import com.daesabu.meongcoach.notification.application.provided.NotificationRegister;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetFinder;
import com.daesabu.meongcoach.notification.application.provided.NotificationTargetResult;
import com.daesabu.meongcoach.notification.application.provided.dto.DeviceInstallationDeleteRequest;
import com.daesabu.meongcoach.notification.application.provided.dto.NotificationDispatchRequest;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.NotificationHour;
import com.daesabu.meongcoach.notification.domain.NotificationSendResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class NotificationDispatchService implements NotificationDispatcher {
	private final NotificationDispatchClaimService claims;
	private final NotificationTargetFinder targets;
	private final NotificationRegister register;
	private final NotificationSender sender;

	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public NotificationDispatchResult dispatch(NotificationDispatchRequest request) {
		NotificationHour.validate(request.hour());
		if (!claims.claim(request.key())) {
			return new NotificationDispatchResult(0, 0, 0, true);
		}
		List<String> fids = targets.findTargets(request.hour()).stream()
				.map(NotificationTargetResult::fid).distinct().toList();
		if (fids.isEmpty()) {
			return new NotificationDispatchResult(0, 0, 0, false);
		}
		NotificationSendResult result = sender.send(fids, request.hour());
		if (!result.invalidFids().isEmpty()) {
			register.deleteDeviceInstallations(new DeviceInstallationDeleteRequest(result.invalidFids()));
		}
		if (result.failure() != null) {
			throw result.failure();
		}
		return new NotificationDispatchResult(fids.size(), result.invalidFids().size(), result.failed(), false);
	}
}
