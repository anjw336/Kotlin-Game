package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.sin

enum class FoodType {
    BASIC, RARE, DEBUFF_SPEED, DEBUFF_SCORE, DEBUFF_TIME, DEBUFF_SPAWN, DEBUFF_SLOW_FALL,
    BUFF_SPEED, BUFF_MULTIPLIER, BUFF_FEVER, BUFF_TIME
}

class FallingObject(
    x: Float,
    y: Float,
    val name: String,
    val type: FoodType,
    val value: Int,          // 점수 혹은 가중치 데이터
    val duration: Float,     // 지속시간
    val imgName: String,
    private val screenHeight: Float,
    var isTyphoonMode: Boolean = false
) : GameObject(x, y, 50f, 50f) {

    private val texture: Texture = Texture(Gdx.files.internal("$imgName.png"))
    private var baseSpeed = 250f
    private var typhoonElapsed = 0f
    private var previousZigzagOffset = 0f
    private val zigzagAmplitude = 75f
    private val zigzagFrequency = 5f

    override fun update(delta: Float) {
        y -= baseSpeed * delta
        if (isTyphoonMode) {
            // 델타 시간에 따라 주기적인 좌우 이동. 이전 오프셋과의 차이만 반영.
            typhoonElapsed += delta
            val offset = sin(typhoonElapsed * zigzagFrequency) * zigzagAmplitude
            x += offset - previousZigzagOffset
            previousZigzagOffset = offset
        } else {
            // 종료 시 누적된 오프셋을 되돌려 다음 이벤트 시작을 안정적으로 처리.
            x -= previousZigzagOffset
            previousZigzagOffset = 0f
            typhoonElapsed = 0f
        }
        x = x.coerceIn(0f, 1024f - width)
    }

    override fun isAlive(): Boolean {
        // 화면 바닥 아래로 완전히 내려가면 소멸 처리
        return y + height > 0
    }

    override fun draw(batch: SpriteBatch) {
        batch.draw(texture, x, y, width, height)
    }

    override fun dispose() {
        texture.dispose()
    }
}