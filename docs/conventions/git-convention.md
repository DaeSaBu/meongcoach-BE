## 1. Git 컨벤션

### 1.1 브랜치

| 브랜치 | 용도 | 규칙 |
|---|---|---|
| `main` | 배포 기준 | 직접 push 금지, `dev`에서만 머지 |
| `dev` | 통합 개발 | 직접 push 금지, PR로만 머지 |
| `feature/{이슈번호}` | 기능 개발 | Linear 이슈 기준 생성, `dev`에서 분기 |

- 브랜치명은 Linear가 제공하는 이슈번호를 그대로 사용한다. (예: `feature/MEO-12`)
- 한 이슈를 여러 브랜치로 나눠야 하는 경우 `feature/MEO-12-2`처럼 뒤에 순번을 붙인다.
- 버그 수정도 별도 브랜치 타입을 두지 않고 Linear에 이슈를 만들어 `feature/{이슈번호}`로 진행한다.

### 1.2 커밋 메시지

[Conventional Commits](https://www.conventionalcommits.org/) 규약을 따른다.

```
<type>(<scope>): <description>

[body(선택)]
```

- **type** (아래 7개만 허용)
	- `feature` : 기능 추가
	- `fix` : 버그 수정
	- `refactor` : 동작 변경 없는 구조 개선
	- `test` : 테스트 코드 추가/수정
	- `docs` : 문서 (RestDocs 포함)
	- `chore` : 빌드, 의존성, CI 설정 등
	- `style` : 포맷팅, 세미콜론 등 (로직 변경 없음)
- **scope** : Modulith 모듈명을 사용한다. (`user`, `walk`, `matching`, `shared`)
	- 여러 모듈에 걸친 변경은 scope를 생략한다.
- description은 한국어로, 명령형으로 작성한다.

```
feature(user): 회원 가입 API 추가
fix(walk): 산책 종료 시간 계산 오류 수정
chore: JaCoCo 커버리지 임계값 조정
```

### 1.3 머지 & PR

- **머지 방식**: Merge commit (커밋 이력을 전부 보존한다. Squash / Rebase 사용 안 함)
	- 커밋이 그대로 dev/main에 남으므로, 커밋 단위를 의미 있게 쪼개고 WIP성 커밋은 push 전에 로컬에서 정리한다.
- **머지 방향**: `feature → dev → main`
- **리뷰**: 최소 1명 승인 필수 (팀 3명 기준)
- **셀프 머지 금지**: 승인 없이는 본인이 머지할 수 없다. (GitHub Branch protection으로 강제)
- **PR 템플릿** (`.github/PULL_REQUEST_TEMPLATE.md`):

```markdown
## 변경 요약

<!-- 무엇을, 왜 변경했는지 -->

## 테스트 방법

<!-- 리뷰어가 확인할 수 있는 방법 (실행 방법, 테스트 코드 위치 등) -->

## 스크린샷

<!-- API 응답, RestDocs 결과 등. 해당 없으면 삭제 -->
```
