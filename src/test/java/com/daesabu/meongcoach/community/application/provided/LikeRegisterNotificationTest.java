package com.daesabu.meongcoach.community.application.provided;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.daesabu.meongcoach.community.application.required.PostRepository;
import com.daesabu.meongcoach.community.domain.Comment;
import com.daesabu.meongcoach.community.domain.Post;
import com.daesabu.meongcoach.community.domain.PostRegisterCommand;
import com.daesabu.meongcoach.notification.application.required.DeviceInstallationRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationEventSettingRepository;
import com.daesabu.meongcoach.notification.application.required.NotificationSender;
import com.daesabu.meongcoach.notification.domain.DeviceInstallation;
import com.daesabu.meongcoach.notification.domain.DeviceInstallationRegisterCommand;
import com.daesabu.meongcoach.notification.domain.DevicePlatform;
import com.daesabu.meongcoach.notification.domain.NotificationDelivery;
import com.daesabu.meongcoach.notification.domain.NotificationEventSetting;
import com.daesabu.meongcoach.notification.domain.NotificationEventType;
import com.daesabu.meongcoach.notification.domain.PushNotification;
import com.daesabu.meongcoach.support.NonTransactionalApplicationTest;
import com.daesabu.meongcoach.user.application.required.UserRepository;
import com.daesabu.meongcoach.user.domain.User;
import com.daesabu.meongcoach.user.domain.UserProfileCreateCommand;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@NonTransactionalApplicationTest
class LikeRegisterNotificationTest {

	private static final long ASYNC_TIMEOUT_MILLIS = 5000;
	private static final long ASYNC_SETTLE_MILLIS = 500;
	private static final String POST_AUTHOR_FID = "like-notification-post-author-fid";
	private static final String ROOT_AUTHOR_FID = "like-notification-root-author-fid";
	private static final String COMMENTER_FID = "like-notification-commenter-fid";
	private static final String PARTICIPANT_FID = "like-notification-participant-fid";

	@Autowired
	private LikeRegister likeRegister;

	@Autowired
	private com.daesabu.meongcoach.community.application.required.CommentRepository comments;

	@Autowired
	private NotificationSender sender;

	@Autowired
	private UserRepository users;

	@Autowired
	private PostRepository posts;

	@Autowired
	private DeviceInstallationRepository installations;

	@Autowired
	private NotificationEventSettingRepository settings;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private TransactionTemplate transaction;
	private final List<Long> userIds = new ArrayList<>();
	private final List<Long> profileIds = new ArrayList<>();
	private final List<List<PushNotification>> batches = new CopyOnWriteArrayList<>();
	private Long postAuthorId;
	private Long rootAuthorId;
	private Long commenterId;
	private Long participantId;
	private Long postId;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		transaction.executeWithoutResult(status -> {
			postAuthorId = saveUser("게시글주인", POST_AUTHOR_FID);
			rootAuthorId = saveUser("댓글주인", ROOT_AUTHOR_FID);
			commenterId = saveUser("멍멍이집사", COMMENTER_FID);
			participantId = saveUser("산책러", PARTICIPANT_FID);
			postId = posts.save(Post.register(postAuthorId, new PostRegisterCommand("오늘 앉아 훈련 성공!", null, List.of()))).getId();
		});
		given(sender.send(anyList())).willAnswer(invocation -> {
			List<PushNotification> notifications = invocation.getArgument(0);
			batches.add(notifications);
			return new NotificationDelivery(notifications.size(), 0, List.of());
		});
	}

	@AfterEach
	void cleanUp() {
		transaction.executeWithoutResult(status -> {
			jdbcTemplate.update("delete from post_likes where post_id = ?", postId);
			jdbcTemplate.update("delete from comment_likes where comment_id in (select id from comments where post_id = ?)", postId);
			jdbcTemplate.update("delete from comment_closures where descendant_id in (select id from comments where post_id = ?)", postId);
			jdbcTemplate.update("delete from comments where post_id = ?", postId);
			jdbcTemplate.update("delete from posts where id = ?", postId);
			userIds.forEach(userId -> {
				jdbcTemplate.update("delete from notification_event_settings where user_id = ?", userId);
				jdbcTemplate.update("delete from device_installations where user_id = ?", userId);
				jdbcTemplate.update("delete from users where id = ?", userId);
			});
			profileIds.forEach(profileId -> jdbcTemplate.update("delete from user_profiles where id = ?", profileId));
		});
		userIds.clear();
		profileIds.clear();
		batches.clear();
	}

	@Test
	void 게시글_좋아요는_작성자에게_한번만_알리고_취소는_알리지_않는다() {
		likeRegister.likePost(commenterId, postId);
		verify(sender, timeout(ASYNC_TIMEOUT_MILLIS)).send(anyList());
		likeRegister.likePost(commenterId, postId);
		likeRegister.unlikePost(commenterId, postId);

		verify(sender, after(ASYNC_SETTLE_MILLIS).times(1)).send(anyList());
		PushNotification notification = batches.getFirst().getFirst();
		assertThat(batches.getFirst()).extracting(PushNotification::fid).containsExactly(POST_AUTHOR_FID);
		assertThat(notification.body()).isEqualTo("게시글에 좋아요가 달렸어요");
		assertThat(notification.type()).isEqualTo(NotificationEventType.LIKE);
		assertThat(notification.deepLink().value()).isEqualTo("meongcoach://community-post/" + postId);
	}

	@Test
	void 댓글_좋아요는_댓글_작성자에게만_한번_알린다() {
		Long commentId = transaction.execute(status -> comments.save(Comment.register(posts.findById(postId).orElseThrow(), rootAuthorId, "댓글")).getId());

		likeRegister.likeComment(commenterId, commentId);
		verify(sender, timeout(ASYNC_TIMEOUT_MILLIS)).send(anyList());
		likeRegister.likeComment(commenterId, commentId);
		likeRegister.unlikeComment(commenterId, commentId);

		verify(sender, after(ASYNC_SETTLE_MILLIS).times(1)).send(anyList());
		assertThat(batches.getFirst()).extracting(PushNotification::fid).containsExactly(ROOT_AUTHOR_FID);
		assertThat(batches.getFirst().getFirst().body()).isEqualTo("댓글에 좋아요가 달렸어요");
		assertThat(batches.getFirst().getFirst().deepLink().value()).isEqualTo("meongcoach://community-post/" + postId);
	}

	@Test
	void 본인_게시글과_댓글_좋아요는_알리지_않는다() {
		Long commentId = transaction.execute(status -> comments.save(Comment.register(posts.findById(postId).orElseThrow(), rootAuthorId, "댓글")).getId());

		likeRegister.likePost(postAuthorId, postId);
		likeRegister.likeComment(rootAuthorId, commentId);

		verify(sender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 좋아요_알림을_끈_회원에게는_보내지_않는다() {
		transaction.executeWithoutResult(status -> jdbcTemplate.update(
				"update notification_event_settings set enabled = false where user_id = ? and type = ?", postAuthorId, "LIKE"));

		likeRegister.likePost(commenterId, postId);

		verify(sender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	@Test
	void 좋아요_트랜잭션이_롤백되면_알리지_않는다() {
		Long commentId = transaction.execute(status -> comments.save(Comment.register(posts.findById(postId).orElseThrow(), rootAuthorId, "댓글")).getId());
		transaction.executeWithoutResult(status -> {
			likeRegister.likePost(commenterId, postId);
			likeRegister.likeComment(commenterId, commentId);
			status.setRollbackOnly();
		});

		verify(sender, after(ASYNC_SETTLE_MILLIS).never()).send(any());
	}

	private Long saveUser(String nickname, String fid) {
		User user = User.registerUser();
		user.completeOnboarding(new UserProfileCreateCommand(nickname, null, "INFP", Set.of(), Set.of()));
		Long userId = users.save(user).getId();
		userIds.add(userId);
		profileIds.add(user.getProfile().getId());
		installations.save(DeviceInstallation.register(userId, new DeviceInstallationRegisterCommand(fid, DevicePlatform.IOS), Instant.now()));
		settings.save(NotificationEventSetting.create(userId, NotificationEventType.LIKE, true));
		return userId;
	}
}
