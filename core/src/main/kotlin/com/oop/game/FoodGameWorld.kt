package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils

class FoodGameWorld : GameWorld(1024f, 768f) {

    private enum class GameState { START, IN_PLAY, GAME_OVER }
    private var currentGameState = GameState.START

    // 시간 및 스테이지 데이터 구조
    private var totalTimer = 180f // 3분 계산
    private var currentStage = 1
    private var gameScore = 0
    private var comboCount = 0

    // 상태 이상 변수 제어
    private var currentStatus = "" // "", "-buff", "-debuff"
    private var statusTimer = 0f
    private var currentSpeedMultiplier = 1.0f
    private var currentScoreMultiplier = 1.0f
    private var currentFallSpeedMultiplier = 1.0f

    // 특수 이벤트 상태량
    private var debuffAcquiredCount = 0
    private var typhoonTimer = 0f
    private var isTyphoonActive = false

    // 일반/희귀 음식을 5회 연속 획득하면 자석 5초 발동
    private var goodFoodStreak = 0
    private var magnetTimer = 0f
    private val magnetDuration = 5f
    private val magnetPullSpeed = 1750f

    // 팰리컨 QTE: 3스테이지, 10~15초 간격, 3초 안에 SPACE 10번
    private var pelicanSpawnTimer = 0f
    private var pelicanSpawnInterval = MathUtils.random(10f, 15f)
    private var isPelicanActive = false
    private var pelicanTimeLeft = 0f
    private var pelicanPressCount = 0
    private var pelicanFeedback = ""
    private var pelicanFeedbackTimer = 0f
    private var pelicanTextureIdle: Texture? = null
    private var pelicanTextureHit: Texture? = null

    private var feverTimer = 0f
    private var isFeverActive = false

    private var spawnAlterTimer = 0f
    private var spawnAlterType = "" // "TOMATO", "BREAD"

    // 패턴 스폰 타이머 설정
    private var basicSpawnTimer = 0f
    private var rareSpawnTimer = 0f
    private var debuffSpawnTimer = 0f
    private var buffSpawnTimer = 0f

    // 에셋 및 개체
    private lateinit var playerObj: Player
    private val backgroundTextures = mutableMapOf<Int, Texture>()

    // 깜빡임 연출 제어
    private var flashTimer = 0f
    private var isFlashRed = false

    override fun show() {
        playerObj = Player(screenWidth / 2 - 50f, 30f, screenWidth)
        add(playerObj)

        backgroundTextures[1] = Texture(Gdx.files.internal("st1_bg.png"))
        backgroundTextures[2] = Texture(Gdx.files.internal("st2_bg.png"))
        backgroundTextures[3] = Texture(Gdx.files.internal("st3_bg.png"))
        pelicanTextureIdle = Texture(Gdx.files.internal("pellican-01.png"))
        pelicanTextureHit = Texture(Gdx.files.internal("pellican-02.png"))
    }

    override fun update(delta: Float) {
        when (currentGameState) {
            GameState.START -> updateStartScreen()
            GameState.IN_PLAY -> updateInGameLogic(delta)
            GameState.GAME_OVER -> updateGameOverScreen()
        }
    }

    private fun updateStartScreen() {
        // 화면 중앙 하단(START! 가상 영역) 터치 혹은 스페이스바로 시작
        if (Gdx.input.justTouched() || InputHandler.isKeyJustPressed(InputHandler.SPACE)) {
            currentGameState = GameState.IN_PLAY
        }
    }

    private fun updateInGameLogic(delta: Float) {
        // 1. 타이머 및 스테이지 전환 체크
        totalTimer -= delta
        if (totalTimer <= 0f) {
            totalTimer = 0f
            endGame()
            return
        }

        // 180초 중 역산하여 스테이지 갱신 요구사항 반영
        val elapsed = 180f - totalTimer
        currentStage = when {
            elapsed <= 40f -> 1
            elapsed <= 100f -> 2
            else -> 3
        }

        // 2. 버프 및 디버프 상태이상 단일 만료 타이머 계산 (중첩 X, 최신 덮어쓰기 보장)
        if (statusTimer > 0f) {
            statusTimer -= delta
            if (statusTimer <= 0f) {
                resetStatusEffects()
            }
        }

        // 특수 독립 타이머 연산
        if (typhoonTimer > 0f) {
            typhoonTimer -= delta
            if (typhoonTimer <= 0f) isTyphoonActive = false
        }
        if (feverTimer > 0f) {
            feverTimer -= delta
            if (feverTimer <= 0f) isFeverActive = false
        }
        if (spawnAlterTimer > 0f) {
            spawnAlterTimer -= delta
            if (spawnAlterTimer <= 0f) spawnAlterType = ""
        }
        if (magnetTimer > 0f) {
            magnetTimer = (magnetTimer - delta).coerceAtLeast(0f)
        }

        updatePelican(delta)

        // 3. 컨트롤러 입력 처리
        var moveSpeed = 450f * currentSpeedMultiplier * delta
        if (InputHandler.isKeyPressed(InputHandler.LEFT)) playerObj.move(-moveSpeed)
        if (InputHandler.isKeyPressed(InputHandler.RIGHT)) playerObj.move(moveSpeed)

        // 4. 아이템 생성 시스템 구동
        handleSpawning(delta)

        // 낙하 → 자석 흡인 → 충돌 및 점수 처리 → 소멸
        getObjects().filterIsInstance<FallingObject>().forEach {
            it.isTyphoonMode = isTyphoonActive
        }
        updateAllObjects(delta)
        handleFoodInteractions(delta)
        removeDead()
    }

    private fun magnetEligible(type: FoodType): Boolean = when (type) {
        FoodType.BASIC, FoodType.RARE,
        FoodType.BUFF_SPEED, FoodType.BUFF_MULTIPLIER,
        FoodType.BUFF_FEVER, FoodType.BUFF_TIME -> true
        else -> false // 디버프 음식은 자석으로 끌어오지 않는다.
    }

    private fun handleFoodInteractions(delta: Float) {
        val targetX = playerObj.x + playerObj.width / 2f
        val targetY = playerObj.y + playerObj.height / 2f
        for (food in getObjects().filterIsInstance<FallingObject>()) {
            if (!food.isAlive()) {
                remove(food)
                food.dispose()
                continue
            }
            val magnetPickup = magnetTimer > 0f && magnetEligible(food.type)
            if (magnetPickup) {
                // 물리 계산만 업데이트하고 렌더링은 render에서 수행한다.
                val dx = targetX - (food.x + food.width / 2f)
                val dy = targetY - (food.y + food.height / 2f)
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance > 0.001f) {
                    val travel = minOf(distance, magnetPullSpeed * delta)
                    food.x += dx / distance * travel
                    food.y += dy / distance * travel
                }
            }
            if (playerObj.collidesWith(food)) {
                processObjectAcquisition(food, fromMagnet = magnetPickup)
                remove(food)
                food.dispose()
            }
        }
    }

    private fun updatePelican(delta: Float) {
        pelicanFeedbackTimer = (pelicanFeedbackTimer - delta).coerceAtLeast(0f)

        if (!isPelicanActive) {
            // 새 이벤트는 3스테이지에서만 시작한다.
            // 와인 효과로 스테이지가 바뀌어도 진행 중인 QTE는 끝까지 처리한다.
            if (currentStage != 3) return
            pelicanSpawnTimer += delta
            if (pelicanSpawnTimer >= pelicanSpawnInterval) {
                isPelicanActive = true
                pelicanPressCount = 0
                pelicanTimeLeft = 3f
                pelicanSpawnTimer = 0f
                pelicanSpawnInterval = MathUtils.random(10f, 15f)
            }
            return
        }

        if (InputHandler.isKeyJustPressed(InputHandler.SPACE)) {
            pelicanPressCount++
            if (pelicanPressCount >= 10) {
                finishPelicanEvent(true)
                return
            }
        }
        pelicanTimeLeft -= delta
        if (pelicanTimeLeft <= 0f) finishPelicanEvent(false)
    }

    private fun finishPelicanEvent(success: Boolean) {
        isPelicanActive = false
        pelicanTimeLeft = 0f
        pelicanFeedback = if (success) "PELICAN DEFENDED!" else "PELICAN STOLE THE FOOD!"
        pelicanFeedbackTimer = 1.8f
        if (!success) {
            getObjects().filterIsInstance<FallingObject>().forEach { food ->
                remove(food)
                food.dispose()
            }
        }
    }

    private fun drawPelicanEvent() {
        if (currentGameState != GameState.IN_PLAY) return
        if (isPelicanActive) {
            val texture = if (pelicanPressCount % 2 == 0) pelicanTextureIdle else pelicanTextureHit
            texture?.let {
                batch.begin()
                batch.draw(it, screenWidth / 2f - 90f, screenHeight - 246f, 180f, 160f)
                batch.end()
            }
            drawTextOnScreen("PELICAN! PRESS SPACE 10 TIMES", screenWidth / 2f - 205f, screenHeight - 252f, Color.YELLOW, 1.25f)
            drawTextOnScreen("${pelicanPressCount}/10 | ${"%.1f".format(java.util.Locale.US, pelicanTimeLeft.coerceAtLeast(0f))}s", screenWidth / 2f - 60f, screenHeight - 284f, Color.WHITE, 1.25f)
        } else if (pelicanFeedbackTimer > 0f) {
            drawTextOnScreen(pelicanFeedback, screenWidth / 2f - 166f, screenHeight - 155f, Color.YELLOW, 1.2f)
        }
    }

    private fun updateGameOverScreen() {
        if (InputHandler.isKeyJustPressed(InputHandler.R)) {
            resetGame()
        }
        if (InputHandler.isKeyJustPressed(InputHandler.ESCAPE)) {
            Gdx.app.exit()
        }
    }

    private fun handleSpawning(delta: Float) {
        basicSpawnTimer -= delta
        rareSpawnTimer -= delta
        debuffSpawnTimer -= delta
        buffSpawnTimer -= delta

        // 기본 아이템 스폰 5~7초 유연화 주기 작동
        if (basicSpawnTimer <= 0f) {
            spawnObjectGroup(FoodType.BASIC)
            basicSpawnTimer = MathUtils.random(5f, 7f)
        }
        if (rareSpawnTimer <= 0f) {
            spawnObjectGroup(FoodType.RARE)
            rareSpawnTimer = 10f
        }
        if (debuffSpawnTimer <= 0f) {
            spawnObjectGroup(FoodType.DEBUFF_SPEED) // 디버프 계열 무작위
            debuffSpawnTimer = if (currentStage == 2) 7f else 10f
        }
        if (buffSpawnTimer <= 0f) {
            spawnObjectGroup(FoodType.BUFF_SPEED) // 버프 계열 무작위
            buffSpawnTimer = 10f
        }
    }

    private fun spawnObjectGroup(baseType: FoodType) {
        // 한 주기당 화면 가로 범위 안에서 다발적으로 흩뿌림 생성 구현
        val spawnCount = MathUtils.random(1, 3)
        for (i in 0 until spawnCount) {
            val rx = MathUtils.random(50f, screenWidth - 100f)
            val ry = screenHeight + MathUtils.random(20f, 150f)

            val generated = ObjectFactory.create(currentStage, baseType, rx, ry, isFeverActive, spawnAlterType)
            generated?.let { add(it) }
        }
    }
    private fun processObjectAcquisition(obj: FallingObject, fromMagnet: Boolean = false) {
        // 자석으로 끌어온 음식을 새로운 연속 획득 횟수에 중복 집계하지 않는다.
        if (!fromMagnet) {
            goodFoodStreak = if (obj.type == FoodType.BASIC || obj.type == FoodType.RARE) {
                goodFoodStreak + 1
            } else 0
            if (goodFoodStreak >= 5) {
                magnetTimer = magnetDuration
                goodFoodStreak = 0
            }
        }
        // 기본 음식 및 희귀 음식 점수 정산 (콤보 누적)
        if (obj.type == FoodType.BASIC || obj.type == FoodType.RARE) {
            comboCount++
            val calculatedScore = (obj.value * currentScoreMultiplier).toInt() + (comboCount * 2)
            gameScore += calculatedScore
        }

        // [수정 포인트] 모든 FoodType enum을 명확히 명시하거나 else 분기를 추가하여 컴파일 에러 해결
        when (obj.type) {
            FoodType.BUFF_SPEED -> {
                applyStatusChange("-buff", 7f)
                currentSpeedMultiplier = 1.2f
            }
            FoodType.BUFF_MULTIPLIER -> {
                applyStatusChange("-buff", 7f)
                currentScoreMultiplier = 1.5f
            }
            FoodType.BUFF_FEVER -> {
                applyStatusChange("-buff", 7f)
                isFeverActive = true
                feverTimer = 7f
            }
            FoodType.BUFF_TIME -> {
                totalTimer += 15f // 와인 효과: 시간 증가
            }
            FoodType.DEBUFF_SPEED -> {
                applyStatusChange("-debuff", obj.duration)
                currentSpeedMultiplier = 0.7f
                incrementDebuffCount()
            }
            FoodType.DEBUFF_SCORE -> {
                applyStatusChange("-debuff", obj.duration)
                gameScore = (gameScore - obj.value).coerceAtLeast(0)
                incrementDebuffCount()
            }
            FoodType.DEBUFF_TIME -> {
                applyStatusChange("-debuff", obj.duration)
                totalTimer = (totalTimer - 5f).coerceAtLeast(0f) // 시간 감소 5초
                incrementDebuffCount()
            }
            FoodType.DEBUFF_SPAWN -> {
                applyStatusChange("-debuff", obj.duration)
                spawnAlterType = if (currentStage == 2) "TOMATO" else "BREAD"
                spawnAlterTimer = 7f
                incrementDebuffCount()
            }
            FoodType.DEBUFF_SLOW_FALL -> {
                applyStatusChange("-debuff", obj.duration)
                currentFallSpeedMultiplier = 0.5f // 먼지 효과: 낙하 속도 감소
                incrementDebuffCount()
            }
            // 혹시 정의되지 않은 다른 FoodType이 들어올 경우를 대비한 방어 코드
            else -> {
                // 아무런 상태 변화 없음
            }
        }

        // 현재 스테이지와 상태 데이터에 맞춰 플레이어 이미지 갱신
        playerObj.updateState(currentStage, currentStatus)
    }

    private fun applyStatusChange(statusKey: String, duration: Float) {
        resetStatusEffects() // 이전 상태 효과 제거 (단일 중첩 제한용)
        currentStatus = statusKey
        statusTimer = duration
    }

    private fun incrementDebuffCount() {
        debuffAcquiredCount++
        if (debuffAcquiredCount >= 5) {
            isTyphoonActive = true
            typhoonTimer = 5f
            debuffAcquiredCount = 0
        }
    }

    private fun resetStatusEffects() {
        currentStatus = ""
        currentSpeedMultiplier = 1.0f
        currentScoreMultiplier = 1.0f
        currentFallSpeedMultiplier = 1.0f
        playerObj.updateState(currentStage, "")
    }

    private fun endGame() {
        currentGameState = GameState.GAME_OVER
        ScoreManager.addScore(gameScore)
    }

    private fun resetGame() {
        getObjects().filterIsInstance<FallingObject>().forEach { food ->
            remove(food)
            food.dispose()
        }
        gameScore = 0
        totalTimer = 180f
        comboCount = 0
        debuffAcquiredCount = 0
        goodFoodStreak = 0
        magnetTimer = 0f
        resetStatusEffects()
        isTyphoonActive = false
        typhoonTimer = 0f
        feverTimer = 0f
        isFeverActive = false
        spawnAlterTimer = 0f
        spawnAlterType = ""
        basicSpawnTimer = 0f
        rareSpawnTimer = 0f
        debuffSpawnTimer = 0f
        buffSpawnTimer = 0f
        currentStage = 1
        playerObj.updateState(1, "")
        isPelicanActive = false
        pelicanSpawnTimer = 0f
        pelicanSpawnInterval = MathUtils.random(10f, 15f)
        pelicanPressCount = 0
        pelicanTimeLeft = 0f
        pelicanFeedback = ""
        pelicanFeedbackTimer = 0f
        currentGameState = GameState.IN_PLAY
    }

    override fun drawBackground(batch: SpriteBatch) {
        val tex = backgroundTextures[currentStage] ?: backgroundTextures[1]
        tex?.let {
            val imgW = it.width.toFloat()
            val imgH = it.height.toFloat()
            val worldRatio = screenWidth / screenHeight
            val imgRatio = imgW / imgH

            var drawW = screenWidth
            var drawH = screenHeight
            var startX = 0f
            var startY = 0f

            // 크롭 및 절대 왜곡 압축 배제 가로 세로 스케일 비율 유지 로직 구현
            if (imgRatio > worldRatio) {
                drawW = screenHeight * imgRatio
                startX = (screenWidth - drawW) / 2f
            } else {
                drawH = screenWidth / imgRatio
                startY = (screenHeight - drawH) / 2f
            }

            // 마감 연출 조건 (종료 20, 10, 5초 전 붉은빛 깜빡임 화면 틴트 결합)
            if (currentGameState == GameState.IN_PLAY && (totalTimer <= 20f && totalTimer > 0f)) {
                flashTimer += Gdx.graphics.deltaTime
                if (flashTimer >= 0.7f) {
                    isFlashRed = !isFlashRed
                    flashTimer = 0f
                }
                if (isFlashRed) {
                    batch.color = Color(1f, 0.5f, 0.5f, 1f)
                }
            } else {
                batch.color = Color.WHITE
            }

            batch.draw(it, startX, startY, drawW, drawH)
            batch.color = Color.WHITE // 렌더 원복
        }
    }

    override fun render(delta: Float) {
        super.render(delta)

        when (currentGameState) {
            GameState.START -> {
                drawTextOnScreen("하늘에서 음식이 내린다면", 320f, 500f, Color.YELLOW, 2.5f)
                drawTextOnScreen("START! (Press SPACE or Click Screen)", 300f, 300f, Color.WHITE, 1.5f)
            }
            GameState.IN_PLAY -> {
                // 좌측 상단 고정: 설명 텍스트
                drawTextOnScreen("Stage 1: Rice(10) Kimchi(20) Mandu(30) | Cola: Speed Up", 20f, screenHeight - 20f, Color.LIGHT_GRAY, 0.9f)
                drawTextOnScreen("Stage 2: Fries(20) Burger(40) Pizza(60) | Beer: Score 1.5x", 20f, screenHeight - 40f, Color.LIGHT_GRAY, 0.9f)

                // 중앙 상단 고정: 스테이지 표기
                drawTextOnScreen("STAGE $currentStage", screenWidth / 2 - 50f, screenHeight - 20f, Color.CYAN, 1.8f)
                if (isTyphoonActive) {
                    drawTextOnScreen("TYPHOON ${"%.1f".format(java.util.Locale.US, typhoonTimer.coerceAtLeast(0f))}s", screenWidth / 2f - 100f, screenHeight - 60f, Color.RED, 1.4f)
                }
                if (magnetTimer > 0f) {
                    drawTextOnScreen("MAGNET ${"%.1f".format(java.util.Locale.US, magnetTimer)}s", screenWidth / 2f - 90f, screenHeight - 92f, Color.YELLOW, 1.3f)
                }

                // 우측 상단 고정: 타이머, 스코어, 콤보
                val minutes = (totalTimer.toInt() / 60)
                val seconds = (totalTimer.toInt() % 60)
                val timeStr = String.format("%02d:%02d", minutes, seconds)

                drawTextOnScreen("TIME: $timeStr", screenWidth - 220f, screenHeight - 20f, Color.WHITE, 1.2f)
                drawTextOnScreen("SCORE: $gameScore", screenWidth - 220f, screenHeight - 45f, Color.GREEN, 1.2f)
                drawTextOnScreen("COMBO: $comboCount", screenWidth - 220f, screenHeight - 70f, Color.ORANGE, 1.2f)
            }
            GameState.GAME_OVER -> {
                // 좌측: 결과 출력부
                drawTextOnScreen("GAME OVER!", 100f, 550f, Color.RED, 3.0f)
                drawTextOnScreen("FINAL SCORE: $gameScore", 100f, 450f, Color.WHITE, 2.0f)
                drawTextOnScreen("Press R to Restart", 100f, 350f, Color.YELLOW, 1.2f)
                drawTextOnScreen("Press ESC to EXIT", 100f, 310f, Color.GRAY, 1.2f)

                // 우측: 랭킹 리스트 출력부
                drawTextOnScreen("==== RANK ====", 650f, 600f, Color.GOLD, 1.5f)
                val tops = ScoreManager.getTop10()
                tops.forEachIndexed { i, rankScore ->
                    drawTextOnScreen("${i + 1}. $rankScore PTS", 650f, 550f - (i * 40f), Color.WHITE, 1.2f)
                }
            }
        }
        drawPelicanEvent()
    }

    override fun dispose() {
        super.dispose()
        backgroundTextures.values.forEach { it.dispose() }
        pelicanTextureIdle?.dispose()
        pelicanTextureHit?.dispose()
    }
}

// 아이템 분기를 효율적으로 관리하기 위한 가상 팩토리 오브젝트 구조
object ObjectFactory {
    fun create(stage: Int, baseType: FoodType, x: Float, y: Float, isFever: Boolean, alterType: String): FallingObject? {
        if (isFever) {
            // 피버 타임 시 무조건 희귀 등급 아이템 강제 매핑 조정
            return when (stage) {
                1 -> FallingObject(x, y, "갈비찜", FoodType.RARE, 500, 0f, "st1-rare-galbi", 768f)
                2 -> FallingObject(x, y, "황금치킨", FoodType.RARE, 500, 0f, "st2-goldchicken", 768f)
                else -> FallingObject(x, y, "조각케이크", FoodType.RARE, 500, 0f, "st3-cake", 768f)
            }
        }

        // 특정 상태 이상 시 빈도 변조 처리
        if (alterType == "TOMATO" && stage == 2) {
            // 건강 음식(토마토 등 가상 매핑 확률 보정 가능)
        }

        return when (stage) {
            1 -> when (baseType) {
                FoodType.BASIC -> FallingObject(x, y, "쌀밥", FoodType.BASIC, 10, 0f, "st1-food-rice", 768f)
                FoodType.RARE -> FallingObject(x, y, "5첩반상", FoodType.RARE, 100, 0f, "st1-rare-5chup", 768f)
                FoodType.DEBUFF_SPEED -> FallingObject(x, y, "상한만두", FoodType.DEBUFF_SPEED, 0, 7f, "st1-de-mandu", 768f)
                else -> FallingObject(x, y, "콜라", FoodType.BUFF_SPEED, 0, 7f, "st1-item-cola", 768f)
            }
            2 -> when (baseType) {
                FoodType.BASIC -> FallingObject(x, y, "감자튀김", FoodType.BASIC, 20, 0f, "st2-fries", 768f)
                FoodType.RARE -> FallingObject(x, y, "엽기떡볶이", FoodType.RARE, 100, 0f, "st2-yeobdduck", 768f)
                FoodType.DEBUFF_SPEED -> FallingObject(x, y, "브로콜리", FoodType.DEBUFF_SPEED, 0, 10f, "st2-de-brcl", 768f)
                else -> FallingObject(x, y, "핫식스", FoodType.BUFF_FEVER, 0, 7f, "st2-item-hot6", 768f)
            }
            3 -> when (baseType) {
                FoodType.BASIC -> FallingObject(x, y, "샐러드", FoodType.BASIC, 30, 0f, "st3-salad", 768f)
                FoodType.RARE -> FallingObject(x, y, "아이스크림", FoodType.RARE, 200, 0f, "st3-icecream", 768f)
                FoodType.DEBUFF_SPEED -> FallingObject(x, y, "더러운 신발", FoodType.DEBUFF_SPEED, 0, 7f, "st3-de-shoes", 768f)
                else -> FallingObject(x, y, "와인", FoodType.BUFF_TIME, 0, 7f, "st3-item-wine", 768f)
            }
            else -> null
        }
    }
}
