# 미디어 업로드 (media 모듈)

media 모듈은 **presigned URL 발급과 저장된 미디어 확인만** 담당하고, 파일은 앱이 스토리지와 직접 주고받습니다. 서버를 파일이 통과하지 않습니다.

## 책임 경계

media는 소비 모듈의 이름이나 용도를 모릅니다. 용도별 규칙은 소비 모듈이 정해 `MediaScope`(media `domain/shared`)와 크기 상한으로 넘기고, media는 공통 규칙만 수행합니다.

| 소비 모듈이 정한다 | media가 정한다 |
|---|---|
| 경로 범위(`MediaScope.path`)와 하위 경로(`withSubPath`) | 객체 키 구조와 그 안의 사용자 ID 위치 |
| 공개 여부(`MediaVisibility`) | 어느 스토리지에 둘지(공개 → R2, 비공개 → S3) |
| 영상 크기 상한 | Content-Type 허용 목록(`ImageType`·`VideoType`), 크기 검증, Content-Length 서명 |
| 언제 발급할지(자격·한도 검사) | 경로 범위 문자열 형식 검증 |

새 업로드 용도를 붙일 때는 **media를 고치지 않습니다.** 소비 모듈에 `MediaScope` 상수와 (영상이면) 크기 상한만 두고 `MediaUploadUrlIssuer`를 호출합니다.

## 공개 API (provided)

| 인터페이스 | 용도 |
|---|---|
| `MediaUploadUrlIssuer` | 업로드 URL 발급. 결과에 공개 URL과 객체 키가 함께 온다 |
| `StoredMediaVerifier.validateUrl` | 클라이언트가 보낸 공개 URL이 **본인이 그 범위에 올린** 파일인지 확인 (`MEDIA_INVALID_URL`) |
| `StoredMediaVerifier.isOwnedObjectKey` | 객체 키가 본인 것인지 확인. 에러 코드는 호출하는 모듈이 고른다 (예: `GRADUATION_INVALID_VIDEO_KEY`) |
| `MediaDownloadUrlIssuer` | 비공개 스토리지 객체의 다운로드 URL 발급. 범위 밖 키면 `MEDIA_INVALID_OBJECT_KEY` |

업로드 완료 알림 없이 클라이언트가 URL을 보내 오므로, **URL을 저장하는 모든 경로에서 `StoredMediaVerifier`로 확인해야 합니다.** 빠뜨리면 외부 URL이나 남이 올린 파일 URL이 그대로 저장됩니다.

## 객체 키 규칙

```
{images|videos}/{경로 범위}/{userId}/[{하위 경로}/]{UUID}.{확장자}
```

**사용자 ID를 고정 위치에 넣는 것이 설계 포인트입니다.** 키만 보고 소유자를 확인할 수 있어 별도 매핑 테이블 없이 소유권을 다룹니다. 하위 경로가 사용자 ID 뒤에 오는 것도 같은 이유입니다. 경로 범위만 다른 키 형식을 따로 만들면 소유자 위치가 모듈마다 달라집니다.

## 경로 범위 등록표

두 모듈이 같은 경로 범위를 쓰면 키 공간이 겹칩니다. 새 범위를 추가하면 이 표에 등록합니다.

| 경로 범위 | 종류·공개 | 영상 상한 | 정의 위치 | 사용처 |
|---|---|---|---|---|
| `user-profile` | 이미지·공개 | — | `onboarding/domain/OnboardingImageTarget` | 온보딩 발급·완료 검증 |
| `dog-profile` | 이미지·공개 | — | `dog/domain/shared/DogProfileImage` | 온보딩 발급, 강아지 등록·수정 검증 |
| `community-post` | 이미지·영상·공개 | 512MiB | `community/domain/PostMediaType` | 게시글 미디어 발급 |
| `training` | 영상·비공개 | 50MiB | `ai/domain/AiVideo` | AI 분석 영상 발급·다운로드 |
| `graduation` + 하위 경로 `{topicId}` | 영상·비공개 | 512MiB | `graduation/domain/GraduationVideo` | 실습 인증 발급·제출 검증·다운로드 |

- 온보딩은 클라이언트가 보낸 target(`USER_PROFILE`·`DOG_PROFILE`)을 직접 범위로 바꿉니다. 클라이언트가 범위를 고를 수 없게 하려는 것이므로, **target 문자열을 media나 범위로 그대로 흘려보내지 않습니다.**
- 크기 위반은 어느 모듈에서 발급하든 `MEDIA_VIDEO_FILE_SIZE_EXCEEDED`·`MEDIA_INVALID_VIDEO_FILE_SIZE`로 내려갑니다. 앱이 이 코드로 안내 문구를 고르므로, 소비 모듈은 상한 값만 정하고 검증은 media에 넘깁니다. 자기 에러 코드로 직접 검증하면 앱 분기가 깨집니다.
- 사진은 크기 상한이 없어 Content-Type만 서명합니다. 상한이 있는 요청만 Content-Length를 서명에 넣고, 이때 앱은 chunked 전송이 아니라 길이를 아는 body로 PUT해야 합니다.

## 스토리지

| 공개 여부 | 스토리지 | 어댑터 | 배포 Secret |
|---|---|---|---|
| 공개 (프로필·게시글) | Cloudflare R2 | `R2ObjectStorage` | `R2_*` |
| 비공개 (AI 분석·실습 인증) | AWS S3 | `S3ObjectStorage` | `S3_*` |

두 스토리지는 자격 증명이 다릅니다. SQS 소비 자격 증명(`SQS_*`)은 S3 presigner와 또 별개입니다. 접두사가 다른 이유는 `application.yml` 주석을 참고하세요. presign은 로컬 서명 연산이라 테스트는 목 없이 실제 어댑터로 URL을 검증합니다.
