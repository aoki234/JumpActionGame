package jp.techacademy.jun.aoki.jumpactiongame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import java.util.*
import kotlin.collections.ArrayList




class GameScreen(private val mGame:JumpActionGame) :ScreenAdapter(){

    companion object {
        val CAMERA_WIDTH = 10f
        val CAMERA_HEIGHT = 15f
        val WORLD_WIDTH = 10f
        val WORLD_HEIGHT = 15 * 20    // 20画面分登れば終了

        val GUI_WIDTH = 320f
        val GUI_HEIGHT = 480f

        val GAME_STATE_READY = 0
        val GAME_STATE_PLAYING = 1
        val GAME_STATE_GAMEOVER = 2

        // 重力
        val GRAVITY = -12
    }

    private val mBg: Sprite
    private val mCamera: OrthographicCamera
    private val mGuiCamera:OrthographicCamera
    private val mViewPort:FitViewport
    private val mGuiViewPort: FitViewport

    private var mRandom: Random
    private var mSteps: ArrayList<Step>
    private var mStars: ArrayList<Star>
    private lateinit var mUfo: Ufo
    private lateinit var mPlayer: Player

    private var mEnemys: ArrayList<Enemy>

    private var mHeightSoFar: Float = 0f
    private var mTouchPoint: Vector3
    private var mGameState:Int

    private var mFont: BitmapFont
    private var mScore: Int
    private var mHighScore: Int

    private var mPrefs: Preferences

    private val sound_enemy: Sound
    private val sound_ground: Sound
    private val sound_star: Sound
    private val sound_ufo: Sound

    init{
        val bgTexture = Texture("back.png")
        mBg = Sprite(TextureRegion(bgTexture,0,0,540,810))
        mBg.setSize(CAMERA_WIDTH, CAMERA_HEIGHT)
        mBg.setPosition(0f,0f)

        //Camera,ViewPortの設定
        mCamera = OrthographicCamera()
        mCamera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT)
        mViewPort = FitViewport(CAMERA_WIDTH, CAMERA_HEIGHT,mCamera)

        //GUI用
        mGuiCamera = OrthographicCamera()
        mGuiCamera.setToOrtho(false, GUI_WIDTH, GUI_HEIGHT)
        mGuiViewPort = FitViewport(GUI_WIDTH, GUI_HEIGHT,mGuiCamera)

        // プロパティの初期化
        mRandom = Random()
        mSteps = ArrayList<Step>()
        mStars = ArrayList<Star>()
        mEnemys = ArrayList<Enemy>()
        mGameState = GAME_STATE_READY

        mTouchPoint = Vector3()


        mFont = BitmapFont(Gdx.files.internal("font.fnt"), Gdx.files.internal("font.png"), false)
        mFont.data.setScale(0.8f)
        mScore = 0
        mHighScore = 0

        mPrefs = Gdx.app.getPreferences("jp.techacademy.jun.aoki.jumpactiongame")
        mHighScore = mPrefs.getInteger("HIGHSOCRE",0)

        //enemyの音
        sound_enemy = Gdx.audio.newSound(Gdx.files.internal("data/enemy.mp3"))
        sound_ground = Gdx.audio.newSound(Gdx.files.internal("data/landing.mp3"))
        sound_star = Gdx.audio.newSound(Gdx.files.internal("data/star.mp3"))
        sound_ufo = Gdx.audio.newSound(Gdx.files.internal("data/ufo.mp3"))



        createStage()
    }

    override fun render(delta: Float) {

        update(delta)

        Gdx.gl.glClearColor(0f,0f,0f,1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        //カメラの移動
        if(mPlayer.y > mCamera.position.y){
            mCamera.position.y = mPlayer.y
        }

        //スライドの表示に反映
        mCamera.update()
        mGame.batch.projectionMatrix = mCamera.combined

        mGame.batch.begin()

        mBg.setPosition(mCamera.position.x - CAMERA_WIDTH / 2, mCamera.position.y - CAMERA_HEIGHT / 2)

        mBg.draw(mGame.batch)

        // Step
        for (i in 0 until mSteps.size) {
            mSteps[i].draw(mGame.batch)
        }

        // Star
        for (i in 0 until mStars.size) {
            mStars[i].draw(mGame.batch)
        }

        //enemy
        for (i in 0 until mEnemys.size) {
            mEnemys[i].draw(mGame.batch)
        }

        // UFO
        mUfo.draw(mGame.batch)

        //Player
        mPlayer.draw(mGame.batch)

        mGame.batch.end()

        // スコア表示
        mGuiCamera.update()
        mGame.batch.projectionMatrix = mGuiCamera.combined
        mGame.batch.begin()
        mFont.draw(mGame.batch, "HighScore: $mHighScore", 16f, GUI_HEIGHT - 15)
        mFont.draw(mGame.batch, "Score: $mScore", 16f, GUI_HEIGHT - 35)
        mGame.batch.end()

    }

    override fun resize(width: Int, height: Int) {
        mViewPort.update(width,height)
        mGuiViewPort.update(width,height)
    }

    // ステージを作成する
    private fun createStage() {

        println("test create stage")
        // テクスチャの準備
        val stepTexture = Texture("step.png")
        val starTexture = Texture("star.png")
        val playerTexture = Texture("uma.png")
        val ufoTexture = Texture("ufo.png")
        val enemyTexture = Texture("enemy.png")

        // StepとStarをゴールの高さまで配置していく
        var y = 0f

        val maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_JUMP_VELOCITY / (2 * -GRAVITY)
        while (y < WORLD_HEIGHT - 5) {
            val type = if(mRandom.nextFloat() > 0.8f) Step.STEP_TYPE_MOVING else Step.STEP_TYPE_STATIC
            val x = mRandom.nextFloat() * (WORLD_WIDTH - Step.STEP_WIDTH)

            val step = Step(type, stepTexture, 0, 0, 144, 36)
            step.setPosition(x, y)
            mSteps.add(step)

            if (mRandom.nextFloat() > 0.6f) {
                val star = Star(starTexture, 0, 0, 72, 72)
                star.setPosition(step.x + mRandom.nextFloat(), step.y + Star.STAR_HEIGHT + mRandom.nextFloat() * 3)
                mStars.add(star)
            }

            //enemyの出現
            if (mRandom.nextFloat() < 0.7f) {
                val enemy = Enemy(enemyTexture, 0, 0, 72, 72)
                enemy.setPosition(step.x/1.5f + mRandom.nextFloat()/2, step.y + Enemy.ENEMY_HEIGHT + mRandom.nextFloat() * 4)
                mEnemys.add(enemy)
            }

            y += (maxJumpHeight - 0.5f)
            y -= mRandom.nextFloat() * (maxJumpHeight / 3)
        }

        // Playerを配置
        mPlayer = Player(playerTexture, 0, 0, 72, 72)
        mPlayer.setPosition(WORLD_WIDTH / 2 - mPlayer.width / 2, Step.STEP_HEIGHT)

        // ゴールのUFOを配置
        mUfo = Ufo(ufoTexture, 0, 0, 120, 74)
        mUfo.setPosition(WORLD_WIDTH / 2 - Ufo.UFO_WIDTH / 2, y)
    }

    // それぞれのオブジェクトの状態をアップデートする
    private fun update(delta: Float) {
        when (mGameState) {
            GAME_STATE_READY ->
                updateReady()
            GAME_STATE_PLAYING ->
                updatePlaying(delta)
            GAME_STATE_GAMEOVER ->
                updateGameover()
        }
    }


    private fun updateReady(){
        if(Gdx.input.justTouched()){
            mGameState = GAME_STATE_PLAYING
            println("test ready touched")
        }
    }

    private fun updatePlaying(delta: Float){
        var accel = 0f
        if(Gdx.input.isTouched) {
            mGuiViewPort.unproject(mTouchPoint.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))
            val left = Rectangle(0f, 0f, GUI_WIDTH / 2, GUI_HEIGHT)
            val right = Rectangle(GUI_WIDTH / 2, 0f, GUI_WIDTH / 2, GUI_HEIGHT)

            if (left.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = 5.0f
            }
            if (right.contains(mTouchPoint.x, mTouchPoint.y)) {
                accel = -5.0f
            }
        }

        for (i in 0 until mSteps.size){
            mSteps[i].update(delta)
        }

        if(mPlayer.y <= 0.5f){
            mPlayer.hitStep()
        }
        mPlayer.update(delta,accel)
        mHeightSoFar = Math.max(mPlayer.y,mHeightSoFar)

        checkCollision()

        checkGameOver()

    }

    private fun updateGameover(){
        if(Gdx.input.justTouched()){
            mGame.screen = ResultScreen(mGame,mScore)
        }
    }


    private fun checkCollision(){

        if(mPlayer.boundingRectangle.overlaps(mUfo.boundingRectangle)){
            sound_ufo.play(1.0f)
            mGameState = GAME_STATE_GAMEOVER
            return

        }

        for (i in 0 until mStars.size){
            val star = mStars[i]

            if(star.mState == Star.STAR_NONE){
                continue
            }
            if(mPlayer.boundingRectangle.overlaps(star.boundingRectangle)){
                star.get()
                sound_star.play(1.0f)

                mScore++
                if(mScore > mHighScore){
                    mHighScore = mScore

                    mPrefs.putInteger("HIGHSCORE",mHighScore)
                    mPrefs.flush()
                }
                break
            }
        }

        //enemyとの当たり判定
        for (i in 0 until mEnemys.size) {
            val enemy = mEnemys[i]

            if (enemy.mState == Enemy.ENEMY_NONE) {
                continue
            }

            if (mPlayer.boundingRectangle.overlaps(enemy.boundingRectangle)) {
                sound_enemy.play(1.0f);
                enemy.get()
                mGameState = GAME_STATE_GAMEOVER
                break
            }
        }

        if(mPlayer.velocity.y > 0){
            return
        }

        for(i in 0 until mSteps.size){
            val step = mSteps[i]

            if(step.mState == Step.STEP_STATE_VANISH){
                continue
            }
            if(mPlayer.boundingRectangle.overlaps(step.boundingRectangle)){
                mPlayer.hitStep()
                if(mRandom.nextFloat() > 0.5f){
                    step.vanish()
                }
                break
            }

        }
    }

    private fun checkGameOver(){
        if(mHeightSoFar - CAMERA_HEIGHT/2 > mPlayer.y){
            sound_ground.play(1.0f)

            Gdx.app.log("JampActionGame","GameOver")
            mGameState = GAME_STATE_GAMEOVER
        }
    }

}