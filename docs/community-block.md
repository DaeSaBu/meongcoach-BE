# 사용자 차단 (community 모듈)

회원이 다른 회원을 차단하면, 차단한 회원의 커뮤니티 조회 응답에서 상대의 게시글·댓글·답글이 빠집니다.
App Store 심사 Guideline 1.2(사용자 생성 콘텐츠)가 요구하는 기능입니다.
API 계약은 [API 문서](../src/docs/asciidoc/index.adoc)의 "사용자 차단" 절에 있습니다.

## 차단은 한 방향입니다

`user_blocks`의 한 행은 "blocker가 blocked의 글을 보지 않는다"만 뜻합니다. 차단당한 회원의 화면은 바뀌지 않고, 차단한 회원의 글에 댓글과 좋아요를 그대로 남길 수 있습니다.
서로 보이지 않게 바꾸려면 아래 조건이 양쪽 방향을 모두 봐야 하고 `(blocked_id, blocker_id)` 인덱스가 더 필요합니다.

## 조회에 차단 조건을 넣는 방법

다른 회원의 글을 내려 주는 조회를 추가하면 `adapter/persistence/UserBlockPredicates.notBlockedBy(viewerId, 작성자 컬럼)`를 where 절에 넣습니다.

```java
queryFactory.selectFrom(comment)
		.where(comment.post.id.eq(postId), idGreaterThan(comment, cursor), isRoot(), comment.deletedAt.isNull(),
				notBlockedBy(viewerId, comment.userId))
```

- **조회한 뒤 서비스에서 걸러내지 않습니다.** 목록은 `id` 커서와 `size + 1`개 조회로 다음 페이지를 판정합니다. 조회 후에 빼면 페이지가 `size`보다 작아지거나 비고, 다음 페이지가 있는데도 `nextCursor`가 null이 될 수 있습니다.
- 조건은 `NOT EXISTS` 상관 서브쿼리입니다. 유니크 제약 `uk_user_blocks_blocker_blocked (blocker_id, blocked_id)`의 인덱스로 조회하므로 인덱스를 따로 두지 않았습니다.
- 차단 엔티티가 community 안에 있어 서브쿼리로 묶을 수 있습니다. 다른 모듈로 옮기면 모듈 경계 때문에 조인할 수 없어, 요청마다 차단한 회원 ID 목록을 먼저 조회해 `NOT IN`으로 넘겨야 합니다.

현재 조건이 들어간 조회는 다음과 같습니다.

| 조회 | 동작 |
|---|---|
| 게시글 목록 | 차단한 회원의 게시글 제외 |
| 게시글 단건 | `PostFinder.findPostDetail`만 404로 거절. 댓글 등록·신고가 함께 쓰는 `findPost(Long)`은 차단을 보지 않음 |
| 루트 댓글 목록, 답글 목록 | 차단한 회원의 댓글·답글 제외. 차단한 회원의 루트 댓글에 달린 다른 회원의 답글도 루트와 함께 보이지 않음 |
| 답글 수(`replyCount`) | 차단한 회원의 답글을 세지 않음 |
| 내가 쓴 댓글 목록 | 차단한 회원의 게시글에 단 내 댓글 제외. 응답에 그 게시글 본문이 실리기 때문 |

## 맞추지 않은 것

- **게시글의 `commentCount`는 차단한 회원의 댓글을 포함합니다.** `posts.comment_count`는 전체 댓글 수를 미리 세어 둔 값이라, 맞추려면 게시글 조회마다 차단한 회원의 댓글 수를 세는 쿼리가 하나 더 필요합니다. 차단한 회원이 댓글을 단 게시글에서만 숫자가 보이는 댓글보다 큽니다.
- 차단한 회원의 게시글에 딸린 댓글 목록 API는 막지 않았습니다. 앱은 게시글 단건 조회가 404라 그 화면에 들어가지 못합니다.
- 탈퇴한 회원의 차단 기록은 지우지 않습니다. 탈퇴 회원의 글이 남는 것과 같습니다.

## 차단·해제

- 차단과 해제는 멱등입니다. 이미 차단한 회원을 다시 차단하거나, 차단하지 않은 회원을 해제해도 204입니다.
- 동시에 들어온 두 차단 요청은 유니크 제약만으로는 하나가 500으로 끝납니다. 저장 전에 차단하는 회원 ID로 트랜잭션 advisory lock을 잡습니다. 키는 [신고](report.md#중복-신고)·쿠폰·AI 분석 횟수와 같은 회원 ID입니다.
- 차단한 사용자 목록은 작성자 정보가 없는 회원(탈퇴, 선택된 강아지 없음)도 `nickname`·`profileImageUrl`이 null인 채로 내려 줍니다. 목록에서 빠지면 그 회원의 차단을 해제할 방법이 없어집니다.

## 알림

- 차단이 커밋되면 `BlockAlertListener`가 alert 모듈로 `사용자 차단`을 보냅니다. 이미 차단한 회원을 다시 차단한 요청은 알리지 않습니다.
- 댓글·좋아요 푸시 알림은 행위자를 차단한 수신자를 빼고 보냅니다(`BlockFinder.findUsersNotBlocking`). 댓글 알림은 본문을 그대로 싣기 때문에, 조회에서만 숨기면 차단한 회원의 글이 푸시로 도착합니다.
