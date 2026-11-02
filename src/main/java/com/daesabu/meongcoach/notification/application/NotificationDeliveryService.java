package com.daesabu.meongcoach.notification.application;

import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.application.required.NotificationSender.NotificationBatchResult;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.NotificationMessage;
import com.daesabu.meongcoach.user.application.provided.UserFinder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDeliveryService {

	private final DeviceInstallationRepository installations;
	private final UserFinder users;
	private final NotificationSender sender;

	@Transactional
	public int deliver(List<Long> userIds, NotificationMessage message) {
		List<String> fids = findFids(userIds);
		if (fids.isEmpty()) {
			return 0;
		}
		Map<String, NotificationMessage> messages = new LinkedHashMap<>();
		fids.forEach(fid -> messages.put(fid, message));
		List<NotificationBatchResult> results = sender.send(messages);
		List<String> invalidFids = results.stream().flatMap(result -> result.invalidFids().stream()).toList();
		deleteInvalidInstallations(invalidFids);
		return fids.size();
	}

	private List<String> findFids(List<Long> userIds) {
		Set<Long> activeUserIds = users.findActiveUserIds(userIds);
		if (activeUserIds.isEmpty()) {
			return List.of();
		}
		return installations.findAllByUserIdInOrderByUserIdAscIdAsc(activeUserIds).stream()
				.map(DeviceInstallation::getFid).distinct().toList();
	}

	private void deleteInvalidInstallations(List<String> invalidFids) {
		if (invalidFids.isEmpty()) {
			return;
		}
		installations.deleteAllByFidIn(invalidFids);
	}
}
