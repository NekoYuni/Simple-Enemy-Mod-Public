package net.nekoyuni.SimpleEnemyMod.entity.client.animation.core;

public enum AnimationPriority {
    CRITICAL(100),  // Death
    HIGH(80),       // Hurt
    MEDIUM(50),     // Walk/Actions
    LOW(20),        // Idle
    NONE(0);

    private final int value;

    AnimationPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
