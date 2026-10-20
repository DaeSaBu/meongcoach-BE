# 미디어 업로드 (media 모듈)

media 모듈은 **presigned URL 발급만** 담당하고, 파일은 앱이 스토리지와 직접 주고받습니다. 서버를 파일이 통과하지 않습니다.

## 스토리지 이원화

| 용도 | 스토리지 | 어댑터 | 배포 Secret |
|---|---|---|---|
| 이미지 (프로필·게시글) | Cloudflare R2 | `R2ImageStorage` | `R2_*` |
| 게시글 영상 | Cloudflare R2 | `R2VideoStorage` | `R2_*` |
| 영상 (AI 분석·실습 인증) | AWS S3 | `S3VideoStorage` | `S3_*` |

공개 URL로 보여주는 파일은 R2에, AI 분석에 쓰는 영상은 S3에 둡니다. 두 스토리지는 자격 증명도 다릅니다. SQS 소비 자격 증명(`SQS_*`)은 S3 presigner와 또 별개입니다 — 접두사가 다른 이유는 `application.yml` 주석 참고.

## 진입점

media는 웹 API를 직접 노출하지 않습니다. 업로드 URL은 업로드가 필요한 모듈이 provided 인터페이스로 발급받아 자기 정책과 함께 노출합니다.

- 이미지 업로드 URL: `onboarding` 모듈이 `ImageUploadUrlIssuer`(provided)로 발급받아 `POST /api/onboarding/presigned-urls`로 노출합니다.
- 게시글 사진·영상 업로드 URL: `community` 모듈이 `CommunityMediaUploadUrlIssuer`(provided)로 발급받아 `POST /api/community/presigned-urls`로 노출합니다. 파일 하나당 한 번 호출합니다.
- 영상 업로드 URL: `ai` 모듈이 `VideoUploadUrlIssuer`(provided)로 발급받아 `POST /api/ai/presigned-urls`로 노출합니다. 업로드 이후의 비동기 분석 흐름은 [ai-pipeline.md](ai-pipeline.md)를 참고하세요.

## 객체 키 규칙

객체 키 형식은 도메인 값 객체가 단일 소유합니다 — 영상은 `VideoObjectKey`(`videos/{대상}/{userId}/{UUID}.{확장자}`), 이미지는 `ImageObjectKey`. **사용자 ID를 키 경로에 넣는 것이 설계 포인트입니다.** 다운로드 URL 발급 시 키만으로 소유자를 검증·식별할 수 있어, 별도 매핑 테이블 없이 소유권을 다룹니다.

실습 인증 영상은 `GraduationVideoUploadUrlIssuer.issue(userId, topicId, contentType, fileSizeBytes)`로 발급합니다. 객체 키는 `videos/graduation/{userId}/{topicId}/{UUID}.{확장자}`이며, 토픽 ID를 명시적으로 받습니다. 기존 `VideoUploadUrlIssuer`는 토픽 ID 없이 기존 영상 업로드 URL을 발급합니다. 두 이슈어는 영상 형식 검증과 `VideoStorage`의 S3 URL 발급 기능을 공유하며, 파일 크기는 각각 검증합니다. 토픽 존재·완료 여부와 실습 인증 자격은 호출하는 graduation 모듈에서 검증합니다.

게시글 미디어 키는 `images/community-post/{userId}/{UUID}.{확장자}`, `videos/community-post/{userId}/{UUID}.{확장자}`입니다. 커뮤니티 이미지는 `ImageUploadTarget`에 값을 두지 않고 `ImageObjectKey.createCommunity`로만 만듭니다. 온보딩 발급 API가 target 문자열을 그대로 넘기므로, enum에 추가하면 온보딩 API로 커뮤니티 키를 발급받을 수 있습니다.

다른 모듈이 이미지 URL을 입력으로 받을 때는 `StoredImageUrlValidator`(provided)로 우리 스토리지의 URL인지 검증합니다.

## 영상 크기 제한

- AI 영상은 `VideoFileSize`에서 최대 50MiB(52,428,800바이트)로 제한합니다.
- 실습 인증 영상은 `GraduationVideoFileSize.MAX_BYTES` 상수로 최대 512MiB(536,870,912바이트)로 제한합니다. 환경 변수는 사용하지 않습니다.
- 게시글 영상은 `CommunityVideoFileSize.MAX_BYTES` 상수로 최대 512MiB로 제한합니다. 게시글 사진은 프로필 이미지처럼 크기 상한이 없습니다.
- 영상 경로는 모두 발급 요청의 파일 크기를 PUT URL의 Content-Length 서명에 포함합니다.
- 실습 인증 앱도 압축 후 512MiB를 검사하고, 화면 안내와 오류 문구에 512MB를 표시합니다. 서버는 URL 발급 시 같은 상한으로 다시 검증합니다. 초과 시 기존 공통 에러 형식의 `MEDIA_VIDEO_FILE_SIZE_EXCEEDED`를 반환하며, S3 업로드와 제출은 시작하지 않습니다.
