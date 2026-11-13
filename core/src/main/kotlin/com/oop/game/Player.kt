package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class Player(x: Float, y: Float, private val worldWidth: Float) : GameObject(x, y, 100f, 100f) {

    // 각 스테이지별 캐릭터 상태 텍스처 캐싱 매핑
    private val textures = mutableMapOf<String, Texture>()

    // 현재 캐릭터가 참조해야 할 상태 키 (기본값 char1.png)
    private var currentTextureKey: String = "char1"

    init {
        val states = listOf("", "-buff", "-debuff")
        for (stage in 1..3) {
            for (state in states) {
                val key = "char$stage$state"
                val fileName = if (stage > 1 && state.isEmpty()) "char$stage-default.png" else "$key.png"
                if (Gdx.files.internal(fileName).exists()) {
                    textures[key] = Texture(Gdx.files.internal(fileName))
                }
            }
        }
    }

    // 외부(World)에서 플레이어의 그래픽 상태를 변경하기 위한 함수
    fun updateState(stage: Int, status: String) {
        // status: "", "-buff", "-debuff"
        currentTextureKey = "char$stage$status"
    }

    override fun update(delta: Float) {
        // 부모 클래스의 속성에서 현재 이동 속도(기본 400f)를 가져와 연산할 수 있도록 설계
        // 실제 유효 속도는 World 단에서 버프/디버프 계수를 곱해 연산 후 주입하거나 여기서 처리
    }

    fun move(moveX: Float) {
        x += moveX
        x = x.coerceIn(0f, worldWidth - width)
    }

    override fun draw(batch: SpriteBatch) {
        val texture = textures[currentTextureKey] ?: textures["char1"]
        texture?.let {
            batch.draw(it, x, y, width, height)
        }
    }

    override fun dispose() {
        textures.values.forEach { it.dispose() }
    }
}
