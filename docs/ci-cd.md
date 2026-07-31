# 백엔드 CI/CD

[.github/workflows/ci.yml](../.github/workflows/ci.yml)이 코드 검증을 담당합니다. [.github/workflows/cd-dev.yml](../.github/workflows/cd-dev.yml)과 [.github/workflows/cd-prod.yml](../.github/workflows/cd-prod.yml)이 환경별 배포를 담당합니다.

## 실행 조건

- `main`, `develop` 대상 PR: CI만 실행
- `develop` push: CI 통과 후 dev 자동 배포
- `main` push: CI 실행
- `main`에서 수동 실행: CI 통과와 입력 검증 후 prod 배포
- draft PR: 실행하지 않고 draft를 해제하면 실행

prod 배포는 health check까지 성공한 dev 배포의 `dev_commit_sha`, `release_version`의 `vMAJOR.MINOR.PATCH`, `confirmation`의 `DEPLOY_PRODUCTION`을 입력해야 합니다. dev와 prod의 Git tree가 같아야 하며 GitHub `production` environment의 필수 승인자를 설정합니다.

## CI

CI는 `workflow_call`을 지원하며 dev·prod CD가 같은 검증 절차를 호출합니다.
`uses: owner/repository@ref`의 `@` 뒤에는 [브랜치·태그·commit SHA](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#jobsjob_idstepsuses)를 쓸 수 있습니다. `@v4`는 같은 major 버전의 다른 commit을 가리키도록 이동할 수 있는 태그이고, 40자리 full commit SHA는 특정 commit을 고정하는 유일한 불변 표기입니다.
외부 Action은 [GitHub 보안 권장사항](https://docs.github.com/en/actions/reference/security/secure-use)에 따라 full commit SHA로 고정하고, 사람이 버전을 알아볼 수 있도록 같은 줄의 주석에 release version을 기록합니다.

1. JDK 25와 Gradle 캐시 설정
2. Spotless 포맷 검사
3. 전체 테스트와 JaCoCo 커버리지 검증
4. JaCoCo HTML 리포트 업로드
5. PR 커버리지 코멘트 작성

라인 커버리지는 70% 이상이어야 합니다. `MeongcoachApplication`과 `shared/config`는 검증에서 제외합니다.
`shared/config`에는 설정만 두고 검증 로직은 커버리지 대상 패키지에 둡니다.

## 배포

| 구분 | dev | prod |
| --- | --- | --- |
| 브랜치 | `develop` | `main` |
| 실행 | push 자동 | 수동 |
| GitHub environment | 없음 | `production` |
| ECR | `meongcoach-dev-ecr` | `meongcoach-prod-ecr` |
| ECS cluster | `meongcoach-dev-cluster` | `meongcoach-prod-cluster` |
| ECS service | `meongcoach-dev-svc` | `meongcoach-prod-svc` |
| AWS role secret | `AWS_DEV_DEPLOY_ROLE_ARN` | `AWS_PROD_DEPLOY_ROLE_ARN` |

배포는 `linux/amd64` 이미지를 커밋 SHA 태그로 ECR에 올립니다. 현재 task definition의 필수 애플리케이션 설정을 검증하고 환경변수와 Secrets Manager 참조를 보존하면서 `api` 이미지와 `SPRING_PROFILES_ACTIVE=dev|prod`만 교체한 뒤, 서비스 안정화 후 환경별 health endpoint를 확인합니다.

## 알림과 실패 확인

- CI는 PR 코멘트, JaCoCo artifact, Actions 로그에서 확인합니다.
- Slack 채널에서 `/github subscribe DaeSaBu/meongcoach-BE workflows:{name:"CI","CD - Dev","CD - Prod"}`로 workflow 알림을 구독합니다.
- 로컬 CI는 `./gradlew spotlessCheck test jacocoTestCoverageVerification`으로 재현합니다.
