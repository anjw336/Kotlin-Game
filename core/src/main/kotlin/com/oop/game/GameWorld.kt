package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * 게임 한 장면 = '월드 하나' 의 추상 기본 클래스.
 *
 * ────────────────────────────────────────────────────────────
 *  왜 이런 게 필요한가?
 * ────────────────────────────────────────────────────────────
 *  게임을 만들 때 학생이 다루는 핵심 개념은 '하나의 월드'다:
 *    - 그 안에 어떤 객체들이 있는가
 *    - 객체들이 매 프레임 어떻게 움직이고 상호작용하는가
 *    - 그것을 어떻게 그릴 것인가
 *  GameWorld 는 이 '월드' 를 표현하는 한 클래스에 모든 것을 담는다.
 *
 *  학생은 이 클래스를 상속해 자기 게임의 월드를 만든다 (ExampleWorld 참고).
 *
 * ────────────────────────────────────────────────────────────
 *  두 가지 크기의 차이
 * ────────────────────────────────────────────────────────────
 *  screenWidth/Height  — 카메라가 한 번에 보여주는 영역 (창 크기와 같다)
 *  worldWidth/Height   — 게임 월드 전체 크기 (화면보다 클 수 있다)
 *  이 둘이 같으면 화면 고정, 월드가 더 크면 카메라(offset) 로 스크롤 가능.
 *
 * ────────────────────────────────────────────────────────────
 *  매 프레임의 표준 흐름 (render 안에서)
 * ────────────────────────────────────────────────────────────
 *    ① 화면 clear
 *    ② update(delta) — 각 객체 갱신, 상호작용, 정리 (서브클래스 override 가능)
 *    ③ batch.begin
 *    ④ drawBackground(batch) — 서브클래스가 그리는 배경 (필수 구현)
 *    ⑤ 모든 게임 객체를 carmera offset 적용해 draw
 *    ⑥ batch.end
 *
 *  학생이 보통 override 하는 것:
 *   ▸ drawBackground(batch) — 자기 배경 그리기 (필수, abstract)
 *   ▸ update(delta)         — 자기 게임 로직 (대부분 override 함)
 *   ▸ render(delta)         — 객체 위에 텍스트/HUD 추가 그리기 (선택)
 *
 * @param screenWidth  화면(카메라)이 보여주는 영역 너비 (픽셀)
 * @param screenHeight 화면(카메라)이 보여주는 영역 높이
 * @param worldWidth   월드 전체 너비 (기본값: 화면과 동일 = 스크롤 없음)
 * @param worldHeight  월드 전체 높이
 */
abstract class GameWorld(
    val screenWidth: Float,
    val screenHeight: Float,
    val worldWidth: Float = screenWidth,
    val worldHeight: Float = screenHeight
) : ScreenAdapter() {

    // OrthographicCamera: 원근 없이(평행 투영) 2D 좌표를 그대로 그려주는 카메라.
    val camera = OrthographicCamera()

    // SpriteBatch: 이미지(Texture) 와 글자를 화면에 찍어주는 도구.
    //   배경 그리기·게임 객체·텍스트 모두 이 batch 하나로 처리한다.
    val batch = SpriteBatch()
    val font = BitmapFont()

    // 카메라 오프셋 — 월드의 어느 지점이 화면 좌하단에 오는지.
    //   이 두 값만 바꾸면 카메라가 움직이는 효과가 난다.
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    // 등록된 객체들만 update/draw 된다.
    // private 으로 감춘 이유: 외부가 직접 add/remove 하면
    //   '순회 중 삭제' 같은 버그가 나기 쉽다. add(), remove() 라는 공식 창구만 허용.
    //   (5주차에서 배운 캡슐화의 실제 사례)
    private val gameObjects = mutableListOf<GameObject>()

    init {
        // 카메라를 '왼쪽 아래 = (0,0), 오른쪽 위 = (screenWidth, screenHeight)' 로 설정.
        //   false 인자는 y 축을 위로(수학 좌표계처럼) 둔다는 뜻.
        camera.setToOrtho(false, screenWidth, screenHeight)
    }

    // ────────────────────────────────────────────────────────
    //  객체 관리
    // ────────────────────────────────────────────────────────

    /** 객체를 월드에 등록 — 이후부터 자동으로 update/draw 된다. */
    fun add(obj: GameObject) {
        gameObjects.add(obj)
    }

    /** 특정 객체를 수동 제거. 보통은 isAlive()=false 후 removeDead() 로 정리. */
    fun remove(obj: GameObject) {
        gameObjects.remove(obj)
    }

    /**
     * 현재 등록된 객체 목록의 '읽기용 복사본'.
     *
     * toList() 로 복사해서 주는 이유:
     *   외부가 받은 리스트에 add/remove 하면 내부 상태가 망가진다.
     *   복사본을 줘서 '훔쳐보기만 하고 건드리진 못하게' 한다.
     */
    fun getObjects(): List<GameObject> = gameObjects.toList()

    // ────────────────────────────────────────────────────────
    //  매 프레임 로직
    // ────────────────────────────────────────────────────────

    /**
     * 등록된 모든 객체에게 'update(delta) 한 프레임 진행' 을 시킨다.
     *
     * 객체 간 상호작용("누가 누구와 부딪혔는가") 은 여기서 결정하지 않는다.
     * 그건 update() 안에서 이 함수를 호출한 뒤 직접 처리할 일이다.
     *
     * TODO (9주차 이후): 고차함수 forEach 로
     *   gameObjects.forEach { it.update(delta) } 처럼 줄일 수 있다.
     */
    protected fun updateAllObjects(delta: Float) {
        for (obj in gameObjects) {
            obj.update(delta)
        }
    }

    /**
     * isAlive() 가 false 인 객체들을 한꺼번에 제거한다.
     *
     * 보통 update() 끝에서 호출 — 상호작용 결과 죽음을 표시한 객체를 정리.
     *
     * 순회 도중 삭제 시 인덱스 꼬임을 막으려고 '먼저 모아 두고 → 한꺼번에 삭제' 패턴.
     *
     * TODO (9주차 이후): 컬렉션 함수 removeAll 로
     *   gameObjects.removeAll { !it.isAlive() } 한 줄로 대체 가능.
     */
    protected fun removeDead() {
        val toRemove = mutableListOf<GameObject>()
        for (obj in gameObjects) {
            if (!obj.isAlive()) {
                toRemove.add(obj)
            }
        }
        for (obj in toRemove) {
            gameObjects.remove(obj)
        }
    }

    /**
     * 매 프레임 게임 로직 — 서브클래스가 override 해서 자기 게임 로직을 넣는 곳.
     *
     * 기본 구현은 가장 단순한 '갱신 → 정리' 시나리오를 보여준다:
     *   ① updateAllObjects(delta) — 각 객체가 자기 위치 갱신
     *   ② removeDead()            — isAlive=false 인 객체 제거
     *
     * 객체 간 상호작용(충돌·점수·생사 결정) 이 있는 게임이면 override 해서
     * 위 두 호출 사이에 그 로직을 끼워 넣는다 (ExampleWorld 참고).
     */
    open fun update(delta: Float) {
        updateAllObjects(delta)
        removeDead()
    }

    // ────────────────────────────────────────────────────────
    //  매 프레임 그리기
    // ────────────────────────────────────────────────────────

    /**
     * LibGDX 가 매 프레임 자동으로 호출.
     *   기본 흐름: 화면 지우기 → 로직 업데이트 → 배경 → 객체.
     *
     * 서브클래스는 보통 update(delta) 만 override 한다.
     * HUD/텍스트를 그리려면 render(delta) 도 override 해서 super 호출 뒤에 그린다.
     */
    override fun render(delta: Float) {
        // 1) 이전 프레임의 잔상 지우기 (검은색으로 채움)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // 2) 카메라 상태 갱신 후, batch 에게 '이 카메라의 좌표계로 그려라' 알려줌
        camera.update()
        batch.projectionMatrix = camera.combined

        // 3) 게임 로직 업데이트
        update(delta)

        // 4) 그리기 — SpriteBatch 는 begin()/end() 사이에서만 동작한다.
        batch.begin()
        drawBackground(batch)
        drawAllObjects()
        batch.end()
    }

    /**
     * 배경을 그리는 자리 — 모든 서브클래스가 반드시 구현해야 한다.
     *
     * 'abstract' 인 이유:
     *   기본 동작('아무것도 안 함') 이 의미 있지 않다. 게임마다 배경은 다르고,
     *   '배경이 없다' 는 결정도 명시적으로 내려야 한다고 본다. 그래서 강제 구현.
     *   (검은 배경을 원하면 그냥 비어있는 함수로 override 하면 됨)
     *
     *   참고: update() 는 abstract 가 아닌 open 이다 — 거기엔 쓸 만한 default 가
     *   존재하기 때문. 'default 가 의미 있는가?' 가 abstract / open 을 가르는 기준.
     *   (7주차 강의 포인트)
     *
     * @param batch 이미 begin() 된 SpriteBatch — 여기에 batch.draw(texture, ...) 로 그린다.
     *              begin/end 를 또 호출하면 안 된다.
     */
    protected abstract fun drawBackground(batch: SpriteBatch)

    /**
     * 등록된 모든 객체를 그린다 — 카메라 오프셋을 반영해서.
     *
     * 핵심 트릭: 객체의 월드 좌표에서 offset 을 잠깐 빼서 '화면 좌표' 처럼 만든 뒤
     *           draw() 시키고, 끝나면 원래 월드 좌표로 되돌린다.
     *
     * 이렇게 해야 서브클래스의 draw() 는 '자기 위치에 그냥 그려라' 만 구현하면 되고,
     *   카메라가 움직이든 말든 신경 쓸 필요가 없다.
     */
    private fun drawAllObjects() {
        for (obj in gameObjects) {
            val originalX = obj.x
            val originalY = obj.y
            obj.x -= offsetX
            obj.y -= offsetY
            obj.draw(batch)
            obj.x = originalX
            obj.y = originalY
        }
    }

    // ────────────────────────────────────────────────────────
    //  텍스트 헬퍼
    // ────────────────────────────────────────────────────────

    /**
     * 화면(카메라) 좌표에 텍스트 그리기.
     *
     * 카메라가 어디로 움직이든 화면상 같은 위치에 고정된다.
     *   → 점수, HP, 남은 시간 같은 UI 에 적합.
     *
     * 주의: 화면 y 축은 위쪽이 크다. 화면 '위쪽'에 글자를 쓰려면 y = screenHeight-10 처럼.
     */
    fun drawTextOnScreen(
        text: String,
        x: Float,
        y: Float,
        color: Color = Color.WHITE,
        scale: Float = 1f
    ) {
        batch.projectionMatrix = camera.combined
        font.color = color
        font.data.setScale(scale)
        batch.begin()
        font.draw(batch, text, x, y)
        batch.end()
    }

    /**
     * 월드 좌표에 텍스트 그리기.
     *
     * 월드의 특정 지점에 고정되므로, 카메라를 움직이면 텍스트도 따라 움직인다.
     *   → 지도 표지판, NPC 머리 위 말풍선, 특정 지역 이름 등에 적합.
     *
     * 구현 원리: 월드 좌표에서 카메라 offset 만큼 빼서 화면 좌표로 바꾼 뒤
     *           drawTextOnScreen 호출.
     */
    fun drawTextInWorld(
        text: String,
        worldX: Float,
        worldY: Float,
        color: Color = Color.WHITE,
        scale: Float = 1f
    ) {
        val screenX = worldX - offsetX
        val screenY = worldY - offsetY
        drawTextOnScreen(text, screenX, screenY, color, scale)
    }

    // ────────────────────────────────────────────────────────
    //  자원 정리
    // ────────────────────────────────────────────────────────

    /**
     * LibGDX 가 화면을 바꾸거나 앱을 종료할 때 자원을 해제한다.
     * GPU 메모리에 올라간 것들은 수동으로 dispose 해줘야 한다.
     */
    override fun dispose() {
        batch.dispose()
        font.dispose()
        for (obj in gameObjects) {
            obj.dispose()
        }
    }
}
