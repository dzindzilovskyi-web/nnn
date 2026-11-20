package com.example.dropall;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class JunkDrop {
    private JunkDrop() {}

    // откуда выкидывать (true = да)
    private static final boolean INCLUDE_CONTAINER = true; // открытый сундук, шалкер и т.д.
    private static final boolean INCLUDE_MAIN = true;      // основной инвентарь
    private static final boolean INCLUDE_HOTBAR = true;    // хотбар

    // точные названия предметов (id без minecraft:)
    private static final Set<String> IDS = new HashSet<>(Arrays.asList(
        "name_tag", "white_banner", "wither_rose", "honey_bottle", "glass_bottle",
        "disc_fragment_5", "pufferfish", "shulker_shell", "enchanted_book", "blaze_rod",
        "sculk", "firework_rocket", "bow", "sculk_sensor", "enchanting_table",
        "leather", "trident", "ghast_tear", "cookie", "ender_chest", "end_crystal",
        "sculk_catalyst", "piston", "water_bucket", "lava_bucket", "milk_bucket",
        "iron_ingot", "nether_wart", "cocoa_beans", "sugar_cane", "cactus",
        "beef", "porkchop", "chicken", "mutton", "rabbit",
        "cooked_beef", "cooked_porkchop", "cooked_chicken", "cooked_mutton", "cooked_rabbit",
        "nautilus_shell", "heart_of_the_sea", "dragon_breath", "echo_shard",
        "spider_eye", "rotten_flesh", "bone", "slime_ball", "gunpowder",
        // новые
        "bone_meal", "anvil", "chipped_anvil", "damaged_anvil", "fermented_spider_eye",
        "gold_ingot", "dirt", "rail", "powered_rail", "detector_rail", "activator_rail",
        "shears", "chest", "flint_and_steel", "wheat_seeds", "soul_sand", "string",
        "bucket", "gold_nugget", "dandelion", "poppy", "target", "slime_block",
        "conduit", "cobbled_deepslate", "deepslate", "book", "hopper", "brewing_stand",
        "raw_iron", "copper_ingot", "arrow"
    ));

    // все виды: отделки, конская броня, лопаты, топоры, яйца призыва
    private static final String[] SUFFIXES = {
        "_armor_trim_smithing_template", "_horse_armor", "_shovel", "_axe", "_spawn_egg"
    };
    // все пластинки
    private static final String[] PREFIXES = { "music_disc_" };
    // стрелы с эффектами по названию
    private static final String[] ARROW_NAMES = {
        "мучительная стрела", "стрела терапии", "стрела обледенения",
        "стрела зевса", "световая стрела", "кровавая стрела"
    };
    // зелья по названию (обычное, взрывное, задерживающееся)
    private static final String[] POTION_NAMES = { "хлопушк" };

    public static boolean isSupported(ScreenHandler h) {
        return h instanceof PlayerScreenHandler
            || h instanceof GenericContainerScreenHandler
            || h instanceof ShulkerBoxScreenHandler
            || h instanceof HopperScreenHandler
            || h instanceof Generic3x3ContainerScreenHandler;
    }

    private static String nameOf(ItemStack stack) {
        return stack.getName().getString()
            .replaceAll("§.", "").replace('ё', 'е').toLowerCase(Locale.ROOT);
    }

    private static boolean isJunk(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();

        if (IDS.contains(path)) return true;
        for (String s : SUFFIXES) if (path.endsWith(s)) return true;
        for (String p : PREFIXES) if (path.startsWith(p)) return true;

        if (path.equals("tipped_arrow")) {
            String name = nameOf(stack);
            for (String a : ARROW_NAMES) if (name.contains(a)) return true;
        }
        if (path.endsWith("potion")) {
            String name = nameOf(stack);
            for (String p : POTION_NAMES) if (name.contains(p)) return true;
        }
        return false;
    }

    // Выкидывает мусор из открытого контейнера, основного инвентаря и хотбара.
    // Броня, щит и сетка крафта не трогаются.
    public static void run(ScreenHandler handler) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        boolean containerOpen = !(handler instanceof PlayerScreenHandler);

        for (Slot slot : handler.slots) {
            if (!isJunk(slot.getStack())) continue;

            if (slot.inventory instanceof PlayerInventory) {
                int idx = slot.getIndex();
                if (idx < 0 || idx > 35) continue;
                if (idx < 9 && !INCLUDE_HOTBAR) continue;
                if (idx >= 9 && !INCLUDE_MAIN) continue;
            } else {
                if (!containerOpen || !INCLUDE_CONTAINER) continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
    }
}
