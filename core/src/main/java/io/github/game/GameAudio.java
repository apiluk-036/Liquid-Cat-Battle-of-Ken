package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/** Background music and click sound, with the volumes set on the SETTINGS panel. */
public class GameAudio {
    private static final String MUSIC_PATH = "sounds/bgfighting.mp3";
    private static final String CLICK_PATH = "sounds/click.mp3";

    private final Music music;
    private final Sound click;
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

    public void playClick() {
        if (click != null) {
            click.play(soundVolume);
        }
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
        if (click != null) {
            click.dispose();
        }
    }
}
