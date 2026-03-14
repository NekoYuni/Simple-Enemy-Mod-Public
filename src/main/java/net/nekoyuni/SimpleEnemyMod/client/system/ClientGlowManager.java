package net.nekoyuni.SimpleEnemyMod.client.system;

import java.util.HashSet;
import java.util.Set;

public class ClientGlowManager {

    private static final Set<Integer> GLOWING_ENTITIES = new HashSet<>();

    public static void addEntity(int id) {
        GLOWING_ENTITIES.add(id);
    }

    public static void removeEntity(int id) {
        GLOWING_ENTITIES.remove(id);
    }

    public static boolean shouldGlow(int id) {
        return GLOWING_ENTITIES.contains(id);
    }

    public static void clear() {
        GLOWING_ENTITIES.clear();
    }

    public static Set<Integer> getAll() {
        return GLOWING_ENTITIES;
    }
}