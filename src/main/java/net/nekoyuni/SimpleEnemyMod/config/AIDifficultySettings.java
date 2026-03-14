package net.nekoyuni.SimpleEnemyMod.config;

public class AIDifficultySettings {

    public final long patienceTimeout;
    public final long flankingDuration;
    public final long coverWaitTime;
    public final double flankSpeed;
    public final double rushSpeed;
    public final double midRangeSpeed;

    private AIDifficultySettings(long patience, long flanking, long cover,
                                 double flank, double rush, double midRange) {
        this.patienceTimeout = patience;
        this.flankingDuration = flanking;
        this.coverWaitTime = cover;
        this.flankSpeed = flank;
        this.rushSpeed = rush;
        this.midRangeSpeed = midRange;
    }

    public static AIDifficultySettings fromConfig() {
        return switch (CommonConfig.DIFFICULTY.get()) {
            case NORMAL -> new AIDifficultySettings(80, 1200, 40, 1.15, 1.3, 1.15);
            case ADVANCED -> new AIDifficultySettings(40, 1200, 15, 1.35, 1.45, 1.2);
        };
    }
}
