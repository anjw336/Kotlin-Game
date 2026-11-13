package com.oop.game.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.oop.game.OopGame

/**
 * ════════════════════════════════════════════════════════════
 *  데스크톱에서 게임을 실행시키는 진입점 (main 함수).
 * ════════════════════════════════════════════════════════════
 *
 *  여기서 하는 일은 세 가지뿐:
 *   1. 게임 객체(OopGame)를 만든다
 *   2. 창(Window) 설정을 만든다
 *   3. LibGDX 에게 "이 게임을 이 설정으로 실행시켜줘" 라고 넘긴다
 *
 *  실제 게임 내용은 OopGame 클래스와 각 Screen 클래스에서 정의된다.
 *  이 파일은 순수 'OS 에 창 띄우기' 역할만 한다.
 */
fun main() {

    // ─────────────────────────────────────────
    // 1) 게임 객체 만들기
    // ─────────────────────────────────────────
    //   OopGame 은 LibGDX 의 Game 을 상속한 클래스 (OopGame.kt 참고).
    //   이 시점에는 단순히 설계도만 들고 있을 뿐, 실제 화면은 아직 안 만들어진다.
    //   화면 생성은 LibGDX 가 나중에 game.create() 를 호출할 때 일어난다.
    val game = OopGame()

    // ─────────────────────────────────────────
    // 2) 창(Window) 설정
    // ─────────────────────────────────────────
    //   창 제목, 크기, FPS 등을 Lwjgl3ApplicationConfiguration 객체에 담는다.
    //   ('Lwjgl3' = Lightweight Java Game Library 3 — LibGDX 의 데스크톱 백엔드)
    //
    // TODO (10주차 이후): 영역함수 'apply' 를 배우면
    //   Lwjgl3ApplicationConfiguration().apply { setTitle(...); setWindowedMode(...); ... }
    //   처럼 더 간결하게 쓸 수 있다.
    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("OOP Game")                                     // 창 제목
    config.setWindowedMode(game.screenWidth, game.screenHeight)     // 창 크기 (OopGame 이 들고 있는 값 사용)
    config.setResizable(false)                                      // 사용자가 창 크기 조절 못하게
    config.useVsync(true)                                           // 수직동기화 (화면 찢어짐 방지)
    config.setForegroundFPS(60)                                     // 최대 60 FPS

    // ─────────────────────────────────────────
    // 3) 실행
    // ─────────────────────────────────────────
    //   Lwjgl3Application 생성자 호출 자체가 전체 게임을 시작시킨다.
    //   내부에서 차례로:
    //     ① OS 창을 띄우고
    //     ② OpenGL 컨텍스트와 Gdx.* 전역을 세팅한 뒤
    //     ③ game.create() 를 호출 → OopGame.create() 안의 setScreen(...) 이 실행되어
    //        ExampleWorld 가 만들어진다
    //     ④ 매 프레임 screen.render(delta) 를 호출하며 루프를 돈다
    //   창이 닫힐 때까지 이 호출은 return 하지 않는다.
    Lwjgl3Application(game, config)
}
