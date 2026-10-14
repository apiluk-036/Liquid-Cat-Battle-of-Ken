package io.github.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 960f;
    private static final float WORLD_HEIGHT = 540f;
    private static final float GRAVITY = 1200f;
    private static final float MOVE_SPEED = 210f;
    private static final float MOVE_ACCELERATION = 900f;
    private static final float MOVE_DECELERATION = 1400f;
    private static final float CAT_JUMP = 500f;
    private static final float GROUND_Y = 30f;

    private static final int STAGE_COUNT = 3;

    /**
     * TITLE -> STAGE_SELECT -> READY (controls shown, waits for ENTER / click) -> PLAYING
     * -> BOSS_DEFEATED (walk to the drop) -> SKILL_CARD -> next stage or CHAMPION.
     * HOW_TO_PLAY is the manual opened from the stage select.
     */
    private enum GameState {
        TITLE, STAGE_SELECT, HOW_TO_PLAY, READY, PLAYING, BOSS_DEFEATED, SKILL_CARD, CHAMPION, LOSE
    }

    private SpriteBatch spriteBatch;
    private Texture backgroundTexture;
    private CatAnimation catAnimation;
    private GameWorld gameWorld;
    private Player player;
    private HpPlayer hpPlayer;
    private SkillEffect skillEffect;
    private FloatingPlatforms floatingPlatforms;
    private float catIdleTime;
    private float blinkTime;
    private Boss boss;
    private GameHud gameHud;
    private StageSelectScreen stageSelect;
    private HowToPlayScreen howToPlay;
    private GameOverScreen gameOver;
    private PauseMenu pauseMenu;
    private StageIntro stageIntro;
    private GameState gameState = GameState.TITLE;
    private PlayerSkills playerSkills;
    private SkillDrop skillDrop;
    private SkillType collectedSkill;
    private int stageIndex;
    /** HP last frame, to play a hurt sound whenever it drops. */
    private int lastPlayerHp;
    private int lastBossHp;
    /** Stages can be beaten in any order; champion only once every boss is down. */
    private final boolean[] stageCleared = new boolean[STAGE_COUNT];
    private GameAudio gameAudio;
    private TitleScreen titleScreen;

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        backgroundTexture = loadTextureIfExists(
            "background.png",
            "background.jpg",
            "blackground.jpg",
            "blackground.png"
        );
        catAnimation = new CatAnimation();
        gameWorld = new GameWorld(WORLD_WIDTH);
        player = new Player(60f, 50f, 34f, 42f);
        hpPlayer = new HpPlayer();
        skillEffect = new SkillEffect();
        gameHud = new GameHud();
        playerSkills = new PlayerSkills();
        stageSelect = new StageSelectScreen(WORLD_WIDTH, WORLD_HEIGHT, STAGE_COUNT);
        howToPlay = new HowToPlayScreen(WORLD_WIDTH, WORLD_HEIGHT);
        gameOver = new GameOverScreen(WORLD_WIDTH, WORLD_HEIGHT);
        stageIntro = new StageIntro(WORLD_WIDTH, WORLD_HEIGHT);
        startStage(0);
        floatingPlatforms = new FloatingPlatforms();
        gameWorld.addOneWayPlatforms(floatingPlatforms.getSurfaces());
        gameAudio = new GameAudio();
        titleScreen = new TitleScreen(WORLD_WIDTH, WORLD_HEIGHT, gameAudio);
        pauseMenu = new PauseMenu(WORLD_WIDTH, WORLD_HEIGHT, gameAudio);
        gameState = GameState.TITLE;
    }

    private Texture loadTextureIfExists(String... paths) {
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                return new Texture(path);
            }
        }
        return null;
    }

    @Override
    public void render() {
        update(Gdx.graphics.getDeltaTime());
        if (gameState == GameState.STAGE_SELECT) {
            stageSelect.draw(spriteBatch, gameHud.getShapeRenderer());
        } else if (gameState == GameState.HOW_TO_PLAY) {
            howToPlay.draw(spriteBatch, gameHud.getShapeRenderer());
        } else {
            drawWorld();
        }
    }

    /** Stage list. Add the next bosses here. */
    private Boss createBoss(int index) {
        if (index == 0) {
            return new DevilBoss(WORLD_WIDTH, GROUND_Y);
        }
        if (index == 1) {
            return new TeacherBoss(WORLD_WIDTH, GROUND_Y);
        }
        return new HackerBoss(WORLD_WIDTH, GROUND_Y);
    }

    /** Starts a stage. Collected skills are kept. The fight waits on the READY panel first. */
    private void startStage(int index) {
        if (boss != null) {
            boss.dispose();
        }
        disposeDrop();
        stageIndex = index;
        boss = createBoss(index);
        player.reset();
        skillEffect.reset();
        playerSkills.resetForStage();
        collectedSkill = null;
        catIdleTime = 0f;
        lastPlayerHp = player.getHp();
        lastBossHp = boss.getHp();
        gameState = GameState.READY;
    }

    /** Lose = back to the stage select, and every collected skill is lost. */
    private void restartGame() {
        resetProgress();
        openStageSelect();
    }

    /** Forgets beaten bosses and collected skills. BACK to the title keeps them; losing or finishing does not. */
    private void resetProgress() {
        playerSkills.clearAll();
        java.util.Arrays.fill(stageCleared, false);
    }

    private void openStageSelect() {
        stageSelect.open(stageCleared);
        gameState = GameState.STAGE_SELECT;
    }

    private boolean allStagesCleared() {
        for (boolean cleared : stageCleared) {
            if (!cleared) {
                return false;
            }
        }
        return true;
    }

    private void update(float delta) {
        switch (gameState) {
            case TITLE:
                if (titleScreen.update()) {
                    openStageSelect();
                }
                return;
            case STAGE_SELECT:
                int chosenStage = stageSelect.update(delta, WORLD_WIDTH, WORLD_HEIGHT);
                if (chosenStage >= 0) {
                    gameAudio.playClick();
                    startStage(chosenStage);
                } else if (chosenStage == StageSelectScreen.BACK) {
                    gameAudio.playClick();
                    gameState = GameState.TITLE;
                } else if (chosenStage == StageSelectScreen.HOW_TO_PLAY) {
                    gameAudio.playClick();
                    howToPlay.open();
                    gameState = GameState.HOW_TO_PLAY;
                }
                return;
            case HOW_TO_PLAY:
                if (howToPlay.update(delta)) {
                    gameAudio.playClick();
                    gameState = GameState.STAGE_SELECT;
                }
                return;
            case READY:
                catIdleTime += delta;
                if (stageIntro.update(delta)) {
                    gameAudio.playClick();
                    gameState = GameState.PLAYING;
                }
                return;
            case SKILL_CARD:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || gameHud.isNextClicked(WORLD_WIDTH, WORLD_HEIGHT)) {
                    if (!allStagesCleared()) {
                        // Pick any boss that is left.
                        openStageSelect();
                    } else {
                        gameState = GameState.CHAMPION;
                    }
                }
                return;
            case LOSE:
                GameOverScreen.Action choice = gameOver.update(delta);
                if (choice == GameOverScreen.Action.YES) {
                    gameAudio.playClick();
                    restartGame();
                } else if (choice == GameOverScreen.Action.NO) {
                    gameAudio.playClick();
                    resetProgress();
                    gameState = GameState.TITLE;
                }
                return;
            case CHAMPION:
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    restartGame();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    resetProgress();
                    gameState = GameState.TITLE;
                }
                return;
            default:
                break;
        }

        if (pauseMenu.update() == PauseMenu.Action.EXIT) {
            openStageSelect();
            return;
        }
        if (pauseMenu.isOpen()) {
            return;
        }

        handleInput(delta);
        player.update(delta);
        blinkTime += delta;
        catIdleTime += delta;

        gameWorld.update(player, delta, GRAVITY);
        float rightLimit = boss.isDefeated() ? WORLD_WIDTH : boss.getHitBox().x;
        player.x = Math.max(0f, Math.min(rightLimit - player.getBodyRightOffset(), player.x));
        skillEffect.update(delta, WORLD_WIDTH);
        boss.update(delta, player);
        if (boss.pollAttackStarted()) {
            gameAudio.playBossAttack();
        }
        playerSkills.update(delta, boss, WORLD_WIDTH);

        if (gameState == GameState.PLAYING) {
            updateCombat();
        } else if (gameState == GameState.BOSS_DEFEATED) {
            updateDropPickup(delta);
        }
        playHurtSounds();
    }

    private void playHurtSounds() {
        if (player.getHp() < lastPlayerHp) {
            gameAudio.playPlayerHurt();
        }
        if (boss.getHp() < lastBossHp) {
            gameAudio.playBossHurt();
        }
        lastPlayerHp = player.getHp();
        lastBossHp = boss.getHp();
    }

    private void updateCombat() {
        int hits = skillEffect.collectHits(boss.getHitBox());
        if (hits > 0) {
            boss.takeDamage(hits * SkillEffect.DAMAGE);
        }

        if (boss.isDefeated()) {
            skillDrop = new SkillDrop(boss.getRewardSkill(), boss.getCenterX() - 60f, boss.getCenterY());
            gameState = GameState.BOSS_DEFEATED;
        } else if (player.isDead()) {
            gameOver.open();
            gameState = GameState.LOSE;
        }
    }

    private void updateDropPickup(float delta) {
        skillDrop.update(delta);
        if (skillDrop.isTouching(player)) {
            stageCleared[stageIndex] = true;
            collectedSkill = skillDrop.getSkill();
            playerSkills.add(collectedSkill);
            disposeDrop();
            gameState = GameState.SKILL_CARD;
        }
    }

    private void disposeDrop() {
        if (skillDrop != null) {
            skillDrop.dispose();
            skillDrop = null;
        }
    }

    private void handleInput(float delta) {
        float horizontal = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            horizontal -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            horizontal += 1f;
        }

        if (horizontal < 0f) {
            player.facing = -1;
        } else if (horizontal > 0f) {
            player.facing = 1;
        }

        float targetVelocityX = horizontal * MOVE_SPEED;
        float changeRate = horizontal == 0f ? MOVE_DECELERATION : MOVE_ACCELERATION;
        player.velocityX = moveTowards(player.velocityX, targetVelocityX, changeRate * delta);

        if ((Gdx.input.isKeyJustPressed(Input.Keys.W) || Gdx.input.isKeyJustPressed(Input.Keys.UP))
            && player.onGround) {
            player.velocityY = CAT_JUMP;
            player.onGround = false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            player.setHp(player.getHp() - 10);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            player.setHp(player.getHp() + 10);
        }
        boolean attacked = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            attacked |= skillEffect.start(player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            attacked |= playerSkills.use(SkillType.SHIELD, player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            attacked |= playerSkills.use(SkillType.GIANT_CANDY, player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            attacked |= playerSkills.use(SkillType.CODE_BREATH, player);
        }
        if (attacked) {
            gameAudio.playPlayerAttack();
        }
    }

    private float moveTowards(float current, float target, float maxChange) {
        if (Math.abs(target - current) <= maxChange) {
            return target;
        }
        return current + Math.signum(target - current) * maxChange;
    }

    private void drawWorld() {
        ScreenUtils.clear(0.08f, 0.12f, 0.14f, 1f);
        if (gameState == GameState.TITLE) {
            titleScreen.draw(spriteBatch);
            return;
        }

        if (backgroundTexture != null) {
            spriteBatch.begin();
            spriteBatch.draw(backgroundTexture, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
            spriteBatch.end();
        }
        hpPlayer.draw(spriteBatch, player, WORLD_HEIGHT);

        spriteBatch.begin();
        floatingPlatforms.draw(spriteBatch);
        boss.drawSprite(spriteBatch);
        TextureRegion catSprite = catAnimation.getIdleFrame(catIdleTime);
        if (catSprite == null) {
            catSprite = getMovingCatSprite();
        }
        boolean blinkHidden = player.isInvincible() && ((int) (blinkTime * 10f)) % 2 == 1;
        if (catSprite != null && !blinkHidden) {
            drawCatSprite(catSprite, player.facing > 0);
        }
        skillEffect.draw(spriteBatch);
        playerSkills.drawSprites(spriteBatch);
        spriteBatch.end();

        ShapeRenderer shapeRenderer = gameHud.getShapeRenderer();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        boss.drawShapes(shapeRenderer);
        playerSkills.drawShapes(shapeRenderer, player);
        if (skillDrop != null) {
            skillDrop.drawShapes(shapeRenderer);
        }
        shapeRenderer.end();

        if (skillDrop != null) {
            spriteBatch.begin();
            skillDrop.drawSprite(spriteBatch);
            spriteBatch.end();
        }

        gameHud.draw(spriteBatch, boss, skillEffect, playerSkills, stageIndex + 1, WORLD_WIDTH, WORLD_HEIGHT);
        if (gameState == GameState.PLAYING || gameState == GameState.BOSS_DEFEATED) {
            pauseMenu.draw(spriteBatch, shapeRenderer);
        }
        if (gameState == GameState.READY) {
            stageIntro.draw(spriteBatch, shapeRenderer, boss, playerSkills, stageIndex + 1);
        } else if (gameState == GameState.SKILL_CARD && collectedSkill != null) {
            gameHud.drawSkillCard(spriteBatch, collectedSkill, !allStagesCleared(),
                WORLD_WIDTH, WORLD_HEIGHT);
        } else if (gameState == GameState.CHAMPION) {
            gameHud.drawChampion(spriteBatch, playerSkills, WORLD_WIDTH, WORLD_HEIGHT);
        } else if (gameState == GameState.LOSE) {
            gameOver.draw(spriteBatch, shapeRenderer);
        }
    }

    private TextureRegion getMovingCatSprite() {
        Texture texture = catAnimation.getMovingTexture(player.facing);
        return texture == null ? null : new TextureRegion(texture);
    }

    private void drawCatSprite(TextureRegion catSprite, boolean flipHorizontal) {
        float drawScale = 1.5f;
        float baseWidth = player.width + 20f;
        float baseHeight = player.height + 20f;
        float drawWidth = baseWidth * drawScale;
        float drawHeight = baseHeight * drawScale;
        float centerX = player.x + baseWidth / 2f;
        float centerY = player.y + baseHeight / 2f;
        float scaleX = flipHorizontal ? -1f : 1f;
        spriteBatch.draw(catSprite, centerX, centerY, drawWidth / 2f, drawHeight / 2f,
            drawWidth, drawHeight, scaleX, 1f, 0f);
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) backgroundTexture.dispose();
        hpPlayer.dispose();
        skillEffect.dispose();
        boss.dispose();
        disposeDrop();
        playerSkills.dispose();
        gameHud.dispose();
        stageSelect.dispose();
        howToPlay.dispose();
        gameOver.dispose();
        pauseMenu.dispose();
        stageIntro.dispose();
        floatingPlatforms.dispose();
        titleScreen.dispose();
        gameAudio.dispose();
        catAnimation.dispose();
        spriteBatch.dispose();
    }
}
