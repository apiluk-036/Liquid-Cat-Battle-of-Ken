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

    private static final int STAGE_COUNT = 2;

    /** PLAYING -> BOSS_DEFEATED (walk to the drop) -> SKILL_CARD -> next stage or CHAMPION. */
    private enum GameState { PLAYING, BOSS_DEFEATED, SKILL_CARD, CHAMPION, LOSE }

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
    private GameState gameState = GameState.PLAYING;
    private PlayerSkills playerSkills;
    private SkillDrop skillDrop;
    private SkillType collectedSkill;
    private int stageIndex;

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
        startStage(0);
        floatingPlatforms = new FloatingPlatforms();
        gameWorld.addOneWayPlatforms(floatingPlatforms.getSurfaces());
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
        drawWorld();
    }

    /** Stage list. Add the next bosses here. */
    private Boss createBoss(int index) {
        if (index == 0) {
            return new DevilBoss(WORLD_WIDTH, GROUND_Y);
        }
        return new TeacherBoss(WORLD_WIDTH, GROUND_Y);
    }

    /** Starts a stage. Collected skills are kept. */
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
        gameState = GameState.PLAYING;
    }

    /** Lose = start over from stage 1, and every collected skill is lost. */
    private void restartGame() {
        playerSkills.clearAll();
        startStage(0);
    }

    private void update(float delta) {
        switch (gameState) {
            case SKILL_CARD:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || gameHud.isNextClicked(WORLD_WIDTH, WORLD_HEIGHT)) {
                    if (stageIndex + 1 < STAGE_COUNT) {
                        startStage(stageIndex + 1);
                    } else {
                        gameState = GameState.CHAMPION;
                    }
                }
                return;
            case CHAMPION:
            case LOSE:
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    restartGame();
                }
                return;
            default:
                break;
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
        playerSkills.update(delta, boss, WORLD_WIDTH);

        if (gameState == GameState.PLAYING) {
            updateCombat();
        } else if (gameState == GameState.BOSS_DEFEATED) {
            updateDropPickup(delta);
        }
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
            gameState = GameState.LOSE;
        }
    }

    private void updateDropPickup(float delta) {
        skillDrop.update(delta);
        if (skillDrop.isTouching(player)) {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            skillEffect.start(player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            playerSkills.use(SkillType.SHIELD, player);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            playerSkills.use(SkillType.GIANT_CANDY, player);
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
        if (gameState == GameState.SKILL_CARD && collectedSkill != null) {
            gameHud.drawSkillCard(spriteBatch, collectedSkill, stageIndex + 1 < STAGE_COUNT,
                WORLD_WIDTH, WORLD_HEIGHT);
        } else if (gameState == GameState.CHAMPION) {
            gameHud.drawChampion(spriteBatch, playerSkills, WORLD_WIDTH, WORLD_HEIGHT);
        } else if (gameState == GameState.LOSE) {
            gameHud.drawGameOver(spriteBatch, WORLD_WIDTH, WORLD_HEIGHT);
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
        floatingPlatforms.dispose();
        catAnimation.dispose();
        spriteBatch.dispose();
    }
}
