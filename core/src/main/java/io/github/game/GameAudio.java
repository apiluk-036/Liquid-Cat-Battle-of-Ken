package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/** Background music, click and combat sounds, with the volumes set on the SETTINGS panel. */
public class GameAudio {
    private static final String MUSIC_PATH = "sounds/bgfighting.mp3";
    private static final String CLICK_PATH = "sounds/click.mp3";

    private final Music music;
    private final Sound click;
    private final Sound playerAttack = loadSound("sounds/player_attack.wav");
    private final Sound bossAttack = loadSound("sounds/boss_attack.wav");
    private final Sound playerHurt = loadSound("sounds/player_hurt.wav");
    private final Sound bossHurt = loadSound("sounds/boss_hurt.wav");
    private float musicVolume = 0.5f;
    private float soundVolume = 0.8f;

    public GameAudio() {
        music = Gdx.files.internal(MUSIC_PATH).exists() ? Gdx.audio.newMusic(Gdx.files.internal(MUSIC_PATH)) : null;
        click = Gdx.files.internal(CLICK_PATH).exists() ? Gdx.audio.newSound(Gdx.files.internal(CLICK_PATH)) : null;
        if (music != null) {
            music.setLooping(true);
            music.setVolume(musicVolume);
            music.play();
        }
    }

    private static Sound loadSound(String path) {
        return Gdx.files.internal(path).exists() ? Gdx.audio.newSound(Gdx.files.internal(path)) : null;
    }

    private void play(Sound sound, float volumeScale) {
        if (sound != null) {
            sound.play(soundVolume * volumeScale);
        }
    }

    public void playClick() {
        play(click, 1f);
    }

    /** Liquid Cat attacks (normal attack or a collected skill). */
    public void playPlayerAttack() {
        play(playerAttack, 0.6f);
    }

    /** A boss starts an attack (laser or special skill). */
    public void playBossAttack() {
        play(bossAttack, 0.6f);
    }

    public void playPlayerHurt() {
        play(playerHurt, 0.8f);
    }

    public void playBossHurt() {
        play(bossHurt, 0.7f);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        if (music != null) {
            music.setVolume(musicVolume);
        }
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float volume) {
        soundVolume = clamp(volume);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public void dispose() {
        if (music != null) {
            music.dispose();
        }
        for (Sound sound : new Sound[] {click, playerAttack, bossAttack, playerHurt, bossHurt}) {
            if (sound != null) {
                sound.dispose();
            }
        }
    }
}
