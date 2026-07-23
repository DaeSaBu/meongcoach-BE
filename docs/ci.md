# CI

GitHub Actions 워크플로우 [.github/workflows/ci.yml](../.github/workflows/ci.yml)이 코드 품질을 검증합니다.

## 트리거

- `main`, `dev` 브랜치 대상 push
- `main`, `dev` 대상 PR의 `opened` / `synchronize`(커밋 추가) / `reopened` / `ready_for_review`(draft 해제)
- **draft PR에서는 실행하지 않는다.** draft를 해제(`ready_for_review`)하면 그 시점에 실행된다.

## CD 연동 (예정)

CI는 `workflow_call` 트리거를 함께 선언해 **재사용 가능한 워크플로우**로 만들어져 있다.
CD를 추가할 때는 CD 워크플로우에서 CI를 선행 job으로 호출하고 배포 job에 `needs`를 걸어, CI 통과 → 배포가 항상 순차적으로 보장되도록 한다.

```yaml
# .github/workflows/cd.yml (예정)
jobs:
  ci:
    uses: ./.github/workflows/ci.yml
  deploy:
    needs: ci        # CI 성공 시에만 배포 실행
    runs-on: ubuntu-latest
    steps: ...
```

## 실행 단계

1. JDK 25 (temurin) 셋업 + Gradle 캐시
2. `./gradlew test jacocoTestCoverageVerification` — 전체 테스트 실행 및 커버리지 검증
3. JaCoCo HTML 리포트를 CI 아티팩트(`jacoco-report`)로 업로드 (실패 시에도 업로드)

## 커버리지 기준

- **라인 커버리지 70% 이상.** 미달 시 빌드가 실패하고 PR을 merge할 수 없습니다.
- 검증 제외 대상: `MeongcoachApplication`(부트스트랩), `shared/config`(설정 클래스) — `build.gradle.kts`의 `jacocoExcludes` 참고
- 기준치·제외 대상 변경은 팀 합의 후 `build.gradle.kts`와 이 문서를 함께 수정합니다.

## 실패 시 확인 방법

- 로컬 재현: `./gradlew test jacocoTestCoverageVerification`
- 커버리지 리포트: `build/reports/jacoco/test/html/index.html` (CI에서는 아티팩트 다운로드)
- 테스트 리포트: `build/reports/tests/test/index.html`
