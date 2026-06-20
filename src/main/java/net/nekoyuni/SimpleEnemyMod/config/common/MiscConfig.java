package net.nekoyuni.SimpleEnemyMod.config.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public class MiscConfig {

    public static ForgeConfigSpec.ConfigValue<String> RECRUIT_ITEM;
    public static ForgeConfigSpec.IntValue RECRUIT_PRICE;

    public static void init(ForgeConfigSpec.Builder builder){

        builder.push("misc_config");

        RECRUIT_ITEM = builder
                .comment("Item ID required to recruit a PMC")
                .define("recruitItem", "minecraft:emerald");

        RECRUIT_PRICE = builder
                .comment("Amount of items required to recruit a PMC")
                .defineInRange("recruitPrice", 16, 1, 64);

        builder.pop();
    }

    public static Item getRecruitItem() {
        ResourceLocation id = new ResourceLocation(RECRUIT_ITEM.get());
        return ForgeRegistries.ITEMS.getValue(id);
    }

}
