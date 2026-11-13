package com.oop.game.example

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.oop.game.GameObject

/**
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  적 예제 — enemy.png 이미지, 수평으로 자동 왕복 이동.
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 *  GameObject 를 상속해 만든 '입력 없이 스스로 움직이는' 객체 예제.
 *
 *  핵심 포인트:
 *   ▸ update() 에 입력 처리가 없다 — AI(자율 행동)는 여기서 작성.
 *   ▸ direction 이라는 '상태 변수'를 둬서 좌/우 방향 전환을 구현.
 *
 *  응용 아이디어:
 *   ▸ 생성자에서 speed 를 받아 FastEnemy, SlowEnemy 로 다양화
 *   ▸ 체력(hp)과 takeDamage() 메서드 추가
 *   ▸ 이동 패턴을 사인파, 원운동 등으로 바꾸기
 *
 * @param minX 왕복 이동의 왼쪽 한계 (보통 0f)
 * @param maxX 왕복 이동의 오른쪽 한계 (보통 worldWidth)
 */
class ExampleEnemy(
    x: Float,
    y: Float,
    private val minX: Float,
    private val maxX: Float
) : GameObject(x, y, 40f, 40f) {

    // 이미지 로딩 — src/main/resources/enemy.png.
    private val texture = Texture(Gdx.files.internal("enemy.png"))

    private val speed = 150f

    // 현재 진행 방향 — +1 이면 오른쪽, -1 이면 왼쪽.
    //   var 로 선언한 이유: 경계에서 반대로 뒤집혀야 하므로 값이 변함.
    private var direction = 1f

    override fun update(delta: Float) {
        // 수평 이동: 속도 × 방향 × 시간
        x += speed * direction * delta

        // 경계에 닿으면 제자리에 붙이고 방향 반전.
        if (x <= minX) {
            x = minX
            direction = 1f
        } else if (x + width >= maxX) {
            x = maxX - width
            direction = -1f
        }
    }

    /**
     * 자신의 이미지를 그린다.
     *   원본은 40x40 이고 width/height 도 40 이라 1:1 로 그려진다.
     *   더 크게 보이게 하려면 width/height 를 늘리면 자동 확대된다.
     */
    override fun draw(batch: SpriteBatch) {
        batch.draw(texture, x, y, width, height)
    }

    override fun dispose() {
        texture.dispose()
    }
}
