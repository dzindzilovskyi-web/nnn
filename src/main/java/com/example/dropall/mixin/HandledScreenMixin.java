package com.example.dropall.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    // Положение кнопок относительно левого верхнего угла окна
    private static final int BTN_OFFSET_Y = -22; // выше окна
    private static final int BTN_HEIGHT = 20;
    private static final int DROP_OFFSET_X = 0;
    private static final int DROP_WIDTH = 90;
    private static final int STACK_OFFSET_X = 94; // правее кнопки «Выкинуть всё»
    private static final int STACK_WIDTH = 90;

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dropall$addButtons(CallbackInfo ci) {
        if ((Object) this instanceof CreativeInventoryScreen) return;
        if (!dropall$isSupported(handler)) return;

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Выкинуть всё"), b -> dropall$dropAll())
                .dimensions(this.x + DROP_OFFSET_X, this.y + BTN_OFFSET_Y, DROP_WIDTH, BTN_HEIGHT)
                .build()
        );
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Сложить всё"), b -> dropall$stackAll())
                .dimensions(this.x + STACK_OFFSET_X, this.y + BTN_OFFSET_Y, STACK_WIDTH, BTN_HEIGHT)
                .build()
        );
    }

    private static boolean dropall$shiftDown() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean dropall$isSupported(ScreenHandler h) {
        return h instanceof PlayerScreenHandler
            || h instanceof GenericContainerScreenHandler   // сундук, большой сундук, эндер-сундук, бочка
            || h instanceof ShulkerBoxScreenHandler
            || h instanceof HopperScreenHandler
            || h instanceof Generic3x3ContainerScreenHandler; // раздатчик, выбрасыватель
    }

    // Слоты, с которыми работают кнопки.
    // Инвентарь игрока: только основные 27 слотов (индексы 9..35), хотбар (0..8), броня и щит не трогаются.
    // Контейнер: его собственные слоты. Shift = вместо контейнера работаем с основным инвентарём игрока.
    private List<Slot> dropall$targetSlots() {
        boolean playerScreen = handler instanceof PlayerScreenHandler;
        boolean usePlayerInv = playerScreen || dropall$shiftDown();

        List<Slot> result = new ArrayList<>();
        for (Slot slot : handler.slots) {
            boolean isPlayerInvSlot = slot.inventory instanceof PlayerInventory;
            if (usePlayerInv) {
                if (!isPlayerInvSlot) continue;
                int idx = slot.getIndex();
                if (idx < 9 || idx > 35) continue;
            } else {
                if (isPlayerInvSlot) continue;
            }
            result.add(slot);
        }
        return result;
    }

    private void dropall$dropAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        boolean playerScreen = handler instanceof PlayerScreenHandler;
        boolean usePlayerInv = playerScreen || dropall$shiftDown();

        for (Slot slot : handler.slots) {
            if (!slot.hasStack()) continue;
            boolean isPlayerInvSlot = slot.inventory instanceof PlayerInventory;

            if (usePlayerInv) {
                // при выкидывании в инвентаре берём и хотбар (0..35)
                if (!isPlayerInvSlot || slot.getIndex() > 35) continue;
            } else {
                if (isPlayerInvSlot) continue;
            }

            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
    }

    private void dropall$stackAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        // если в курсоре что-то лежит, не начинаем, чтобы ничего не перепутать
        if (!handler.getCursorStack().isEmpty()) return;

        List<Slot> slots = dropall$targetSlots();

        outer:
        for (int a = 0; a < slots.size(); a++) {
            Slot sa = slots.get(a);
            for (int b = a + 1; b < slots.size(); b++) {
                ItemStack stackA = sa.getStack();
                if (stackA.isEmpty()) continue outer;
                if (stackA.getCount() >= stackA.getMaxCount()) continue outer;

                Slot sb = slots.get(b);
                ItemStack stackB = sb.getStack();
                if (stackB.isEmpty() || !ItemStack.areItemsAndComponentsEqual(stackA, stackB)) continue;

                // берём стак B, кладём в A, остаток возвращаем в B
                dropall$click(mc, sb);
                dropall$click(mc, sa);
                if (!handler.getCursorStack().isEmpty()) {
                    dropall$click(mc, sb);
                }
            }
        }

        // на случай, если в курсоре что-то осталось, вернуть в первый подходящий слот
        if (!handler.getCursorStack().isEmpty()) {
            for (Slot s : slots) {
                if (s.getStack().isEmpty()) {
                    dropall$click(mc, s);
                    break;
                }
            }
        }
    }

    private void dropall$click(MinecraftClient mc, Slot slot) {
        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
    }
}
