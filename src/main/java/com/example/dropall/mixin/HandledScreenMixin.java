package com.example.dropall.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    // Раскладка: ряд кнопок над окном, по центру окна.
    // Порядок слева направо: Выкинуть всё | Сложить всё | Сортировать | Забрать всё
    // Между «Сложить» и «Сортировать» находится центр окна (Сложить чуть левее центра).
    private static final int BTN_W = 72;
    private static final int BTN_H = 20;
    private static final int GAP = 2;
    private static final int OFFSET_ABOVE = 22; // на сколько выше окна

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    @Unique private ButtonWidget dropall$dropBtn;
    @Unique private ButtonWidget dropall$stackBtn;
    @Unique private ButtonWidget dropall$sortBtn;
    @Unique private ButtonWidget dropall$takeBtn;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dropall$addButtons(CallbackInfo ci) {
        dropall$dropBtn = dropall$stackBtn = dropall$sortBtn = dropall$takeBtn = null;
        if ((Object) this instanceof CreativeInventoryScreen) return;
        if (!dropall$isSupported(handler)) return;

        dropall$dropBtn = ButtonWidget.builder(Text.literal("Выкинуть всё"), b -> dropall$dropAll())
            .dimensions(0, 0, BTN_W, BTN_H).build();
        dropall$stackBtn = ButtonWidget.builder(Text.literal("Сложить всё"), b -> dropall$stackMyInventory())
            .dimensions(0, 0, BTN_W, BTN_H).build();
        dropall$sortBtn = ButtonWidget.builder(Text.literal("Сортировать"), b -> dropall$sortOpened())
            .dimensions(0, 0, BTN_W, BTN_H).build();

        this.addDrawableChild(dropall$dropBtn);
        this.addDrawableChild(dropall$stackBtn);
        this.addDrawableChild(dropall$sortBtn);

        // «Забрать всё» только там, где есть контейнер
        if (!(handler instanceof PlayerScreenHandler)) {
            dropall$takeBtn = ButtonWidget.builder(Text.literal("Забрать всё"), b -> dropall$takeAll())
                .dimensions(0, 0, BTN_W, BTN_H).build();
            this.addDrawableChild(dropall$takeBtn);
        }
        dropall$layout();
    }

    // Окно может сдвинуться (например, при открытии книги рецептов), поэтому пересчитываем каждый кадр
    @Inject(method = "render", at = @At("HEAD"))
    private void dropall$onRender(CallbackInfo ci) {
        dropall$layout();
    }

    @Unique
    private void dropall$layout() {
        if (dropall$dropBtn == null) return;
        int step = BTN_W + GAP;
        int total = 4 * BTN_W + 3 * GAP;
        int center = this.x + this.backgroundWidth / 2;
        int start = center - total / 2;
        int by = Math.max(2, this.y - OFFSET_ABOVE);

        dropall$dropBtn.setPosition(start, by);
        dropall$stackBtn.setPosition(start + step, by);
        dropall$sortBtn.setPosition(start + 2 * step, by);
        if (dropall$takeBtn != null) dropall$takeBtn.setPosition(start + 3 * step, by);
    }

    @Unique
    private static boolean dropall$shiftDown() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Unique
    private static boolean dropall$isSupported(ScreenHandler h) {
        return h instanceof PlayerScreenHandler
            || h instanceof GenericContainerScreenHandler   // сундук, большой сундук, эндер-сундук, бочка
            || h instanceof ShulkerBoxScreenHandler
            || h instanceof HopperScreenHandler
            || h instanceof Generic3x3ContainerScreenHandler; // раздатчик, выбрасыватель
    }

    // ---------- наборы слотов ----------

    /** Слоты открытого контейнера (не инвентарь игрока). */
    @Unique
    private List<Slot> dropall$containerSlots() {
        List<Slot> r = new ArrayList<>();
        for (Slot s : handler.slots) {
            if (!(s.inventory instanceof PlayerInventory)) r.add(s);
        }
        return r;
    }

    /** Основной инвентарь игрока: 27 слотов, БЕЗ хотбара, брони и щита. */
    @Unique
    private List<Slot> dropall$mainInventorySlots() {
        List<Slot> r = new ArrayList<>();
        for (Slot s : handler.slots) {
            if (s.inventory instanceof PlayerInventory) {
                int idx = s.getIndex();
                if (idx >= 9 && idx <= 35) r.add(s);
            }
        }
        return r;
    }

    // ---------- Выкинуть всё ----------

    @Unique
    private void dropall$dropAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        boolean usePlayerInv = handler instanceof PlayerScreenHandler || dropall$shiftDown();

        for (Slot slot : handler.slots) {
            if (!slot.hasStack()) continue;
            boolean isPlayerInvSlot = slot.inventory instanceof PlayerInventory;
            if (usePlayerInv) {
                if (!isPlayerInvSlot || slot.getIndex() > 35) continue;
            } else {
                if (isPlayerInvSlot) continue;
            }
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
    }

    // ---------- Забрать всё ----------

    @Unique
    private void dropall$takeAll() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        for (Slot slot : dropall$containerSlots()) {
            if (!slot.hasStack()) continue;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    // ---------- Сложить всё (ваш инвентарь без хотбара) ----------

    @Unique
    private void dropall$stackMyInventory() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (!handler.getCursorStack().isEmpty()) return;
        dropall$merge(mc, dropall$mainInventorySlots());
    }

    // ---------- Сортировать (открытое меню; в инвентаре игрока - основной инвентарь) ----------

    @Unique
    private void dropall$sortOpened() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (!handler.getCursorStack().isEmpty()) return;

        List<Slot> slots = (handler instanceof PlayerScreenHandler)
            ? dropall$mainInventorySlots()
            : dropall$containerSlots();

        // 1) объединяем неполные стаки
        dropall$merge(mc, slots);

        // 2) целевой порядок: по id предмета, пустые в конце
        List<ItemStack> desired = new ArrayList<>();
        for (Slot s : slots) {
            if (!s.getStack().isEmpty()) desired.add(s.getStack().copy());
        }
        desired.sort(Comparator.comparing((ItemStack st) -> Registries.ITEM.getId(st.getItem()).toString()));

        // 3) расставляем обменами
        for (int i = 0; i < desired.size(); i++) {
            Slot si = slots.get(i);
            ItemStack want = desired.get(i);
            ItemStack have = si.getStack();
            if (!have.isEmpty() && ItemStack.areItemsAndComponentsEqual(have, want)) continue;

            for (int j = i + 1; j < slots.size(); j++) {
                Slot sj = slots.get(j);
                ItemStack cand = sj.getStack();
                if (!cand.isEmpty() && ItemStack.areItemsAndComponentsEqual(cand, want)) {
                    if (!have.isEmpty()) dropall$click(mc, si);
                    dropall$click(mc, sj);
                    dropall$click(mc, si);
                    break;
                }
            }
        }

        if (!handler.getCursorStack().isEmpty()) {
            for (Slot s : slots) {
                if (s.getStack().isEmpty()) { dropall$click(mc, s); break; }
            }
        }
    }

    // ---------- общее: объединение неполных стаков внутри списка слотов ----------

    @Unique
    private void dropall$merge(MinecraftClient mc, List<Slot> slots) {
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

                dropall$click(mc, sb);
                dropall$click(mc, sa);
                if (!handler.getCursorStack().isEmpty()) {
                    dropall$click(mc, sb);
                }
            }
        }
    }

    @Unique
    private void dropall$click(MinecraftClient mc, Slot slot) {
        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
    }
}
