package net.nekoyuni.SimpleEnemyMod.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY_SEM = "key.category.simpleenemymod.general";
    public static final String KEY_COMMANDER_MENU = "key.simpleenemymod.commander_menu";

    public static final KeyMapping COMMANDER_MENU_KEY = new KeyMapping(
            KEY_COMMANDER_MENU,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KEY_CATEGORY_SEM
    );
}
