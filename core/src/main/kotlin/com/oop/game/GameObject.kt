package com.oop.game

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

/**
 * 게임에 등장하는 모든 '무엇인가'의 공통 부모.
 *
 * ────────────────────────────────────────────────────────────
 *  왜 이런 게 필요한가?
 * ────────────────────────────────────────────────────────────
 *  Player, Enemy, Bullet 은 기능은 다르지만
 *    - 화면의 특정 위치(x, y)에 있고
 *    - 어떤 크기(width, height)를 가지고
 *    - 매 프레임 스스로 상태를 갱신(update)하고
 *    - 자신을 그릴 줄(draw) 안다
 *  이 '공통 속성/행동'을 한 곳에 모아둔 것이 GameObject 이다.
 *
 *  GameWorld 는 이 GameObject 타입으로만 객체들을 관리한다.
 *  즉, 우리가 Player 든 Bullet 이든 **GameObject를 상속**하기만 하면
 *  GameWorld 가 자동으로 update/draw/제거까지 해준다 (다형성).
 *
 * ────────────────────────────────────────────────────────────
 *  사용법 — 새로운 게임 객체 만들기
 * ────────────────────────────────────────────────────────────
 *    class Bullet(x: Float, y: Float) : GameObject(x, y, 8f, 16f) {
 *        private val texture = Texture(Gdx.files.internal("bullet.png"))
 *        override fun update(delta: Float) { y += 400f * delta }
 *        override fun draw(batch: SpriteBatch) {
 *            batch.draw(texture, x, y, width, height)
 *        }
 *        override fun dispose() { texture.dispose() }
 *    }
 *
 * @param x      왼쪽 아래 꼭짓점의 월드 좌표 x
 * @param y      왼쪽 아래 꼭짓점의 월드 좌표 y
 * @param width  가로 크기 (픽셀)
 * @param height 세로 크기 (픽셀)
 */
abstract class GameObject(
    // var 로 선언한 이유: 객체는 게임 중 위치가 **변해야 하므로**.
    //   val 로 만들면 한 번 생성된 이후 움직일 수 없다.
    //   파이썬의 self.x = ... 와 같은 역할을 val/var 속성이 한다.
    var x: Float,
    var y: Float,
    // 크기는 일반적으로 바뀌지 않지만, 폭발·성장 이펙트 같은 확장을 위해 var.
    //   바뀔 일이 없다면 val 로 두는 것이 더 안전하다.
    var width: Float,
    var height: Float
) {

    /**
     * 이 객체가 아직 '살아있는지' 여부.
     *
     * GameWorld 가 매 프레임 removeDead() 를 호출하면,
     *   이 값이 false 인 객체가 월드에서 정리된다.
     *
     * 기본값은 true — 대부분의 객체는 '살아있는 게 기본' 이기 때문.
     * 'open' 이므로 서브클래스에서 원한다면 override 할 수 있다.
     *   예) class Bullet(val worldHeight: Float) {
     *           override fun isAlive() = y in 0f..worldHeight   // 화면 안에 있을 때만 살아있음
     *       }
     */
    open fun isAlive(): Boolean = true

    /**
     * 매 프레임 호출되어 **상태를 갱신**한다.
     *
     * abstract 인 이유: 객체마다 '움직이는 방식'이 완전히 다르기 때문.
     *   Player 는 키 입력을 읽고, Enemy 는 AI 로직을 돌리고, Bullet 은 직진한다.
     *   공통 구현을 한 가지로 정할 수 없으니, 서브클래스가 강제로 구현하게 한다.
     *
     * @param delta 직전 프레임과의 시간 간격(초). 60fps 면 약 0.0167.
     *              '픽셀/초' 단위의 속도에 delta 를 곱하면 '이번 프레임 이동량' 이 된다.
     *              (프레임 속도가 달라져도 같은 속도로 움직이게 하려는 공식)
     */
    abstract fun update(delta: Float)

    /**
     * 매 프레임 호출되어 **자신을 그린다**.
     *
     * @param batch SpriteBatch — 이미지(Texture)를 화면에 찍어주는 도구.
     *              GameWorld 가 이미 projectionMatrix 를 세팅하고
     *              begin()/end() 안에서 호출해주므로, 서브클래스는
     *              batch.draw(texture, x, y, w, h) 한 줄만 적으면 된다.
     *
     * 이미지 로딩은 보통 객체의 init 또는 프로퍼티 초기화 시점에 한 번 한다:
     *   private val texture = Texture(Gdx.files.internal("player.png"))
     */
    abstract fun draw(batch: SpriteBatch)

    /**
     * 이 객체가 차지하는 사각형 영역 — 충돌 판정에 쓴다.
     *
     * 매번 새 Rectangle 을 만든다. 성능이 극한으로 중요한 곳이라면 재사용해야
     * 하지만, 이 강의의 규모에서는 가독성을 더 우선한다.
     */
    fun getBounds(): Rectangle = Rectangle(x, y, width, height)

    /**
     * 다른 객체와 충돌했는지 검사 — AABB(축 정렬 경계 상자) 방식.
     *
     * 두 사각형이 한 픽셀이라도 겹치면 true.
     *   더 정밀한 판정(원, 다각형, 픽셀 단위)이 필요하면 서브클래스에서
     *   별도 메서드를 만들거나 이 메서드를 override 해서 바꿀 수 있다.
     *
     * 왜 GameObject 에 둘까?
     *   모든 게임 객체가 '충돌할 수 있다' 는 공통 능력을 가지기 때문.
     *   그래서 player.collidesWith(enemy), bullet.collidesWith(wall) 처럼
     *   어떤 조합이든 똑같은 문법으로 쓸 수 있다.
     */
    fun collidesWith(other: GameObject): Boolean {
        return getBounds().overlaps(other.getBounds())
    }

    /**
     * 이 객체가 갖고 있는 GPU 자원을 정리한다 — 화면이 닫힐 때 한 번 호출된다.
     *
     * 왜 필요한가?
     *   Texture, Sound 같은 LibGDX 자원은 GPU/네이티브 메모리를 점유한다.
     *   garbage collector 는 이 메모리를 해제해 주지 못한다 → dispose() 명시적 호출 필요.
     *
     * 기본 구현은 빈 함수 — Texture 같은 자원을 안 쓰는 객체는 그대로 두면 된다.
     * 텍스처를 쓰는 객체라면 override 해서 texture.dispose() 를 호출.
     */
    open fun dispose() {}
}
