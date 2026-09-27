# 교육 카드 댓글

교육 카드에 달리는 댓글·답글·좋아요는 `cardcomment` 모듈이 소유한다. 기준 코드는 `src/main/java/com/daesabu/meongcoach/cardcomment`다.

## 모듈 경계

- `cardcomment`는 `training`과 `user`에 단방향으로 의존한다. `training`의 `CardFinder`로 카드 존재를 확인하고, `user`의 `UserProfileFinder`로 작성자 닉네임을 받는다.
- `training`은 `cardcomment`를 참조하지 않는다. 카드 목록 응답에 댓글 수가 없는 이유다. 앱은 댓글 수 일괄 조회 API로 레슨에 속한 카드들의 댓글 수를 한 번에 받는다.
- 댓글 도메인은 `Card`·`Lesson`·`User` 엔티티를 참조하지 않고 ID 값만 저장한다. `architecture/LayerDependencyTest`가 `cardcomment.domain`의 `training`·`user` 참조를 막는다.
- 카탈로그와 분리한 이유는 댓글이 사용자 생성 콘텐츠이기 때문이다. 카탈로그 옆에 사용자 데이터를 따로 두는 것은 `progress`와 같은 방식이다.

## 커뮤니티 댓글과의 경계

- 카드 댓글과 앞으로 생길 커뮤니티 게시글 댓글은 다른 모델로 둔다. 스레드·좋아요 메커니즘은 같지만 대상의 소유와 생명주기, 앞으로 붙을 규칙이 다르다.
- 카드 댓글은 다른 모듈이 소유한 관리자 콘텐츠에 붙고, 대상에 작성자가 없다. 훈련사 답변·고정 댓글·정렬 같은 규칙이 붙을 자리다.
- 커뮤니티 댓글은 `community` 모듈에서 게시글과 함께 만든다. 게시글과 같은 모듈이라 게시글 삭제와 함께 처리할 수 있다.
- 두 댓글에 공통으로 걸리는 신고·차단은 댓글 모델이 아니라 모더레이션 기능으로 공유한다. 신고는 콘텐츠 종류와 ID로 대상을 가리키고, 두 댓글 모듈은 조회할 때 차단 목록으로 걸러낸다. 모더레이션의 위치와 형태는 정하지 않았다.

## 데이터 모델

테이블은 두 개다. 스키마의 원천은 `db/migration/V6__create_card_comments.sql`이다.

| 테이블 | 역할 | 주요 컬럼 |
|---|---|---|
| `card_comments` | 댓글과 답글 | `card_id`, `author_id`, `root_id`, `parent_id`, `content` |
| `card_comment_likes` | 좋아요 | `comment_id`, `user_id` 복합 PK |

- 댓글과 답글은 한 테이블에 둔다. `root_id`가 NULL이면 최상위 댓글이다.
- `root_id`는 스레드의 최상위 댓글이고 `parent_id`는 실제로 답한 댓글이다. 최상위 댓글에 단 답글은 둘이 같다.
- 답글에 답글을 달면 `root_id`는 그 답글의 `root_id`, `parent_id`는 그 답글이다. 스레드는 한 단계로 평평하게 유지하되 답한 대상은 남긴다. 본문에 `@닉네임`을 넣어 대상을 표시하지 않는다.
- `card_id`·`author_id`는 다른 모듈의 엔티티를 가리키므로 FK와 JPA 연관을 두지 않는다. `root_id`·`parent_id`·`comment_id`는 같은 테이블 안의 참조라 FK를 건다.
- 댓글 수·좋아요 수는 카운터 컬럼 없이 조회할 때 집계한다.
- 카드가 삭제될 때 댓글을 어떻게 할지는 정하지 않았다. 카드 삭제 기능이 생길 때 결정한다.

## 읽기 모델

댓글 한 건의 응답은 댓글·답글 구분 없이 같은 모양이다. `id`, `parentId`, `authorId`, `authorNickname`, `mine`, `content`, `createdAt`, `replyCount`, `likeCount`, `likedByMe`.

- `CardCommentRepository`의 쿼리 하나가 답글 수·좋아요 수·내 좋아요 여부를 서브쿼리로 집계해 한 페이지를 만든다. 닉네임만 `user` 모듈에서 작성자 ID 집합으로 한 번에 받아 합친다.
- 최상위 댓글 목록과 답글 목록은 같은 페이지 형식이다. `comments`, `totalCount`, `nextCursor`.
- 최상위 댓글은 `id` 내림차순, 답글은 `id` 오름차순으로 커서 페이지네이션한다. 커서는 마지막 항목의 `id`이고 다음 페이지가 없으면 `nextCursor`가 null이다.
- 한 페이지는 20건이다. 페이지 크기는 클라이언트가 정하지 않는다.
- `totalCount`는 최상위 댓글 목록에서는 답글을 포함한 카드 전체 댓글 수, 답글 목록에서는 스레드의 답글 수다.
- `authorNickname`은 작성자 프로필의 닉네임이다. 댓글 API는 온보딩을 마친 USER 역할만 호출할 수 있어 프로필이 없는 작성자는 정상 상태가 아니며 그 경우에만 null이다.
- `mine`은 작성자가 요청한 사용자인지 여부다. 삭제·수정 기능의 전제 조건이다.

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/training/cards/{cardId}/comments?cursor` | 최상위 댓글 한 페이지. `totalCount`는 카드 전체 댓글 수 |
| GET | `/api/training/comments/{commentId}/replies?cursor` | 최상위 댓글의 답글 한 페이지. `totalCount`는 스레드 답글 수 |
| GET | `/api/training/comments/counts?cardIds=` | 카드별 댓글 수 일괄 조회. 댓글이 없는 카드는 응답에 없다 |
| POST | `/api/training/cards/{cardId}/comments` | 댓글 작성 |
| POST | `/api/training/comments/{commentId}/replies` | 답글 작성. 대상이 답글이면 같은 스레드에 붙인다 |
| POST | `/api/training/comments/{commentId}/like` | 좋아요. 멱등 |
| DELETE | `/api/training/comments/{commentId}/like` | 좋아요 취소. 멱등 |

- 좋아요는 `(comment_id, user_id)` PK가 중복을 막는다. 이미 누른 좋아요를 다시 눌러도 상태는 같다.
- 본문은 앞뒤 공백을 지운 뒤 1~500자여야 한다. `CardComment`가 검증하고 `COMMENT_INVALID_CONTENT`(400)를 던진다.
- 없는 카드는 `COMMENT_CARD_NOT_FOUND`(404)다. 없는 댓글과 답글 ID로 요청한 답글 목록은 `COMMENT_NOT_FOUND`(404)다.

## 정하지 않은 것

- 댓글 삭제: 소프트 딜리트 여부, 답글이 남은 댓글의 표시, 집계 제외 방식.
- 댓글 수정: "수정됨" 표시 여부, 수정 가능 기간.
- 신고·차단: 앱 스토어 UGC 심사 기준을 맞추는 데 필요하다. 커뮤니티 댓글과 함께 쓰는 모더레이션 기능으로 붙인다.
- 알림: 댓글·답글 작성 시 `cardcomment` 모듈이 이벤트를 발행하고 알림 모듈이 구독하는 방식으로 붙인다. `Card`와 `CardComment` 사이에 양방향 참조를 두지 않는다.
