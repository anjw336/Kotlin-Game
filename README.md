# 하늘에서 음식이 내린다면

Kotlin과 LibGDX로 만든 **음식 받기 아케이드 게임**입니다. 객체지향프로그래밍 수업 프로젝트로, 좌우로 움직이며 떨어지는 음식을 모아 3분 동안 높은 점수에 도전합니다.

![게임 배경 이미지](core/src/main/resources/st1_bg.png)

## 게임 소개

- 시간에 따라 바뀌는 3개 스테이지와 음식·배경
- 음식 획득과 콤보를 통한 점수 누적
- 이동 속도와 점수 등에 영향을 주는 버프·디버프
- 태풍과 피버 이벤트
- 게임 종료 후 상위 10개 점수 저장

## 실행 방법

**JDK 17**이 필요합니다. 포함된 Gradle Wrapper를 사용하므로 Gradle은 별도로 설치하지 않아도 됩니다. 첫 실행에는 인터넷 연결이 필요합니다.

```bash
git clone https://github.com/anjw336/Kotlin-Game.git
cd Kotlin-Game
```

Windows PowerShell:

```powershell
.\gradlew.bat desktop:run
```

macOS / Linux:

```bash
chmod +x gradlew
./gradlew desktop:run
```

IntelliJ IDEA에서는 프로젝트 폴더를 열고 Gradle 동기화 후 `desktop > Tasks > application > run`을 실행합니다. macOS의 `-XstartOnFirstThread` 옵션은 Gradle 설정에 포함되어 있습니다.

## 조작 방법

| 입력 | 동작 |
| --- | --- |
| Space 또는 화면 클릭 | 게임 시작 |
| ← / → | 플레이어 좌우 이동 |
| R | 게임 종료 화면에서 재시작 |
| Esc | 게임 종료 화면에서 종료 |

## 프로젝트 구조

```text
Kotlin-Game/
├── core/
│   └── src/main/
│       ├── kotlin/com/oop/game/
│       │   ├── OopGame.kt       # 첫 게임 화면 설정
│       │   ├── FoodGameWorld.kt # 게임 상태, 스테이지, 아이템 생성
│       │   ├── GameWorld.kt     # 화면과 렌더링 공통 기반
│       │   ├── GameObject.kt    # 게임 객체와 충돌 판정 공통 기반
│       │   ├── Player.kt        # 플레이어 이동과 상태 표현
│       │   ├── FallingObject.kt # 낙하 객체와 음식 종류
│       │   ├── InputHandler.kt  # 키보드 입력 도우미
│       │   ├── ScoreManager.kt  # 상위 10개 점수 저장
│       │   └── example/         # 수업용 기본 예제
│       └── resources/          # 캐릭터, 음식, 배경 PNG
├── desktop/                    # 데스크톱 실행 진입점
├── gradle/wrapper/             # Gradle Wrapper
├── build.gradle.kts
└── settings.gradle.kts
```

`GameWorld`를 상속한 `FoodGameWorld`가 게임을 진행하고, `GameObject`를 상속한 `Player`와 `FallingObject`가 공통 갱신·그리기 흐름에 참여합니다. `InputHandler`와 `ScoreManager`는 Kotlin `object`로 관리합니다.

## 개발 환경

| 항목 | 버전 |
| --- | --- |
| Java | JDK 17 |
| Kotlin | 1.9.22 |
| LibGDX | 1.12.1 |
| Gradle Wrapper | 8.5 |
| 데스크톱 백엔드 | LWJGL3 |

빌드 확인:

```powershell
.\gradlew.bat build
```

macOS / Linux에서는 `./gradlew build`를 사용합니다. `desktop:installDist`로 배포용 실행 스크립트와 의존성을 생성할 수 있습니다.

랭킹은 실행 작업 디렉터리의 `gamedata/rankings.txt`에 저장되며 Git에서 제외됩니다. IDE 설정과 빌드 결과도 저장소에 포함하지 않습니다.

## 현재 참고 사항

기본 `BitmapFont`를 사용하므로 한글 제목과 일부 특수문자는 정상적으로 표시되지 않을 수 있습니다. 한글을 표시하려면 한글 글리프를 포함한 폰트 적용이 필요합니다.
