package com.example.dropall;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class JunkDrop {
    private JunkDrop() {}

    // true = выкидывать мусор и из хотбара тоже
    private static final boolean INCLUDE_HOTBAR = false;

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
        "spider_eye", "rotten_flesh", "bone", "slime_ball", "gunpowder"
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

    public static boolean isSupported(ScreenHandler h) {
        return h instanceof PlayerScreenHandler
            || h instanceof GenericContainerScreenHandler
            || h instanceof ShulkerBoxScreenHandler
            || h instanceof HopperScreenHandler
            || h instanceof Generic3x3ContainerScreenHandler;
    }

    private static boolean shiftDown() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isJunk(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();

        if (IDS.contains(path)) return true;
        for (String s : SUFFIXES) if (path.endsWith(s)) return true;
        for (String p : PREFIXES) if (path.startsWith(p)) return true;

        if (path.equals("tipped_arrow")) {
            String name = stack.getName().getString()
                .replaceAll("§.", "").replace('ё', 'е').toLowerCase(Locale.ROOT);
            for (String a : ARROW_NAMES) if (name.contains(a)) return true;
        }
        return false;
    }

    // Обычное нажатие: выкидывает мусор из вашего основного инвентаря (без хотбара).
    // С зажатым Shift в сундуке и т.п.: выкидывает мусор из самого контейнера.
    public static void run(ScreenHandler handler) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        boolean fromContainer = !(handler instanceof PlayerScreenHandler) && shiftDown();

        for (Slot slot : handler.slots) {
            if (!isJunk(slot.getStack())) continue;
            boolean isPlayerInv = slot.inventory instanceof PlayerInventory;

            if (fromContainer) {
                if (isPlayerInv) continue;
            } else {
                if (!isPlayerInv) continue;
                int idx = slot.getIndex();
                int min = INCLUDE_HOTBAR ? 0 : 9;
                if (idx < min || idx > 35) continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
    }
}
