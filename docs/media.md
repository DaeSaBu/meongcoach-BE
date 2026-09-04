# 미디어 업로드 (media 모듈)

media 모듈은 **presigned URL 발급만** 담당하고, 파일은 앱이 스토리지와 직접 주고받습니다. 서버를 파일이 통과하지 않습니다.

## 스토리지 이원화

| 용도 | 스토리지 | 어댑터 | 배포 Secret |
|---|---|---|---|
| 이미지 (프로필 등) | Cloudflare R2 | `R2ImageStorage` | `R2_*` |
| 영상 (AI 분석용) | AWS S3 | `S3VideoStorage` | `S3_*` |

이미지와 영상은 스토리지도 자격 증명도 다릅니다. SQS 소비 자격 증명(`SQS_*`)은 S3 presigner와 또 별개입니다 — 접두사가 다른 이유는 `application.yml` 주석 참고.

## 진입점

media는 웹 API를 직접 노출하지 않습니다. 업로드 URL은 업로드가 필요한 모듈이 provided 인터페이스로 발급받아 자기 정책과 함께 노출합니다.

- 이미지 업로드 URL: `onboarding` 모듈이 `ImageUploadUrlIssuer`(provided)로 발급받아 `POST /api/onboarding/presigned-urls`로 노출합니다.
- 영상 업로드 URL: `ai` 모듈이 `VideoUploadUrlIssuer`(provided)로 발급받아 `POST /api/ai/presigned-urls`로 노출합니다. 업로드 이후의 비동기 분석 흐름은 [ai-pipeline.md](ai-pipeline.md)를 참고하세요.

## 객체 키 규칙

객체 키 형식은 도메인 값 객체가 단일 소유합니다 — 영상은 `VideoObjectKey`(`videos/{대상}/{userId}/{UUID}.{확장자}`), 이미지는 `ImageObjectKey`. **사용자 ID를 키 경로에 넣는 것이 설계 포인트입니다.** 다운로드 URL 발급 시 키만으로 소유자를 검증·식별할 수 있어, 별도 매핑 테이블 없이 소유권을 다룹니다.

다른 모듈이 이미지 URL을 입력으로 받을 때는 `StoredImageUrlValidator`(provided)로 우리 스토리지의 URL인지 검증합니다.
