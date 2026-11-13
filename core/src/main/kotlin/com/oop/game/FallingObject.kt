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
    private var timeElapsed = 0f
    private val zigzagAmplitude = 150f // 태풍 시 좌우 변동 폭
    private val zigzagFrequency = 5f   // 지그재그 주기

    override fun update(delta: Float) {
        timeElapsed += delta

        // 위에서 아래로 하락
        y -= baseSpeed * delta

        // 태풍 모드 발동 시 크게 지그재그 모션 추가
        if (isTyphoonMode) {
            x += sin(timeElapsed * zigzagFrequency) * zigzagAmplitude * delta
        }
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