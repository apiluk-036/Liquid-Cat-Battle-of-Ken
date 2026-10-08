package io.github.game;

/** Skills a boss drops. Once collected, the player can use them in the next stages. */
public enum SkillType {
    GIANT_CANDY("Giant Candy", "OOP", "skills/candy.png", "F", 10f,
        "Throw a giant candy forward", "Damage 50", "Cooldown 10 s"),
    SHIELD("Shield", "Dis", "skills/shield.png", "Q", 5f,
        "Protect area around you", "Invincible 3 s", "Cooldown 5 s");

    public final String displayName;
    public final String subject;
    public final String iconPath;
    public final String key;
    public final float cooldown;
    public final String[] details;

    SkillType(String displayName, String subject, String iconPath, String key, float cooldown,
              String... details) {
        this.displayName = displayName;
        this.subject = subject;
        this.iconPath = iconPath;
        this.key = key;
        this.cooldown = cooldown;
        this.details = details;
    }
}
