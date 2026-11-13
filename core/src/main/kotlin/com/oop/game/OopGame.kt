package com.oop.game

import com.badlogic.gdx.Game

/**
 * LibGDX 게임의 실제 메인 진입점 클래스입니다.
 * 원래 내장되어 있던 ExampleWorld 대신, 새로 구현한 FoodGameWorld를 첫 화면으로 세팅합니다.
 */
class OopGame : Game() {

    val screenWidth = 1024
    val screenHeight = 768

    override fun create() {
        // 기존 ExampleWorld() 대신 우리가 만든 FoodGameWorld()를 할당하여 게임을 시작합니다.
        setScreen(FoodGameWorld())
    }

    override fun dispose() {
        super.dispose()
    }
}

