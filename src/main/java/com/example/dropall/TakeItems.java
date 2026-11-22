package com.example.dropall;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TakeItems {
    private TakeItems() {}

    // точные названия предметов (id без minecraft:): серебро = железный кусочек
    private static final Set<String> IDS = new HashSet<>(Arrays.asList(
        "iron_nugget"
    ));

    // все виды голов и черепов (player_head, zombie_head, skeleton_skull и т.д.)
    private static final String[] SUFFIXES = { "_head", "_skull" };

    private static boolean wanted(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (IDS.contains(path)) return true;
        for (String s : SUFFIXES) if (path.endsWith(s)) return true;
        return false;
    }

    // Забирает нужные предметы из открытого контейнера в ваш инвентарь (как Shift+клик).
    public static void run(ScreenHandler handler) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (handler instanceof PlayerScreenHandler) return;

        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) continue;
            if (!wanted(slot.getStack())) continue;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}
