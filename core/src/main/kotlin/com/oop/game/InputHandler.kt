package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

/**
 * 키보드 입력을 편리하게 읽는 도우미.
 *
 * ────────────────────────────────────────────────────────────
 *  왜 이런 게 필요한가?
 * ────────────────────────────────────────────────────────────
 *  LibGDX 에서 키 입력을 확인하려면 매번
 *      Gdx.input.isKeyPressed(Input.Keys.LEFT)
 *  처럼 길게 적어야 한다. 게임 로직 안에서 이 긴 표현이 반복되면
 *  코드가 지저분해지고 핵심 흐름이 잘 보이지 않는다.
 *
 *  그래서 자주 쓰는 입력만 모아 짧은 이름으로 감싸둔다.
 *      InputHandler.isKeyPressed(InputHandler.LEFT)
 *
 * ────────────────────────────────────────────────────────────
 *  object 키워드
 * ────────────────────────────────────────────────────────────
 *  'object' 는 **싱글톤 객체** 를 만드는 키워드다.
 *    - 인스턴스가 단 하나뿐이고
 *    - 어디서든 InputHandler.XXX 로 바로 접근 가능
 *  입력처럼 "상태는 시스템 하나에만 존재" 하는 자원에 잘 어울린다.
 *  (파이썬이라면 모듈 수준 함수/변수와 비슷한 역할)
 */
object InputHandler {

    /**
     * 키가 현재 '눌려 있는 중' 인지 — 꾹 누르고 있으면 매 프레임 true.
     *   이동(← → ↑ ↓) 처럼 '누르는 동안 계속' 일어나야 할 동작에 사용.
     */
    fun isKeyPressed(key: Int): Boolean {
        return Gdx.input.isKeyPressed(key)
    }

    /**
     * 키가 '이번 프레임에 막 눌렸는지' — 꾹 눌러도 첫 프레임에만 true.
     *   총알 발사, 메뉴 선택처럼 '한 번만' 실행되어야 할 동작에 사용.
     */
    fun isKeyJustPressed(key: Int): Boolean {
        return Gdx.input.isKeyJustPressed(key)
    }

    // 자주 쓰는 키 상수를 짧은 이름으로 재노출.
    //   원본은 Input.Keys.LEFT 처럼 길어서 자주 쓸수록 번거롭다.
    //   필요하면 Input.Keys.XXX 에서 다른 키를 직접 import 해서 써도 된다.
    val LEFT = Input.Keys.LEFT
    val RIGHT = Input.Keys.RIGHT
    val UP = Input.Keys.UP
    val DOWN = Input.Keys.DOWN
    val SPACE = Input.Keys.SPACE
    val ESCAPE = Input.Keys.ESCAPE
    val W = Input.Keys.W
    val A = Input.Keys.A
    val S = Input.Keys.S
    val D = Input.Keys.D
    val R = Input.Keys.R
}
