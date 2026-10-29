package com.example.dropall.mixin;

import com.example.dropall.DropAllHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    protected InventoryScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dropall$addButtons(CallbackInfo ci) {
        int bw = 80;
        int bh = 20;
        int gap = 4;
        int left = (this.width - 176) / 2;
        int top = (this.height - 166) / 2;

        int dropX = left - bw - gap;
        if (dropX < 2) dropX = 2;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Выкинуть все"), button -> DropAllHelper.dropPlayerItems())
                        .dimensions(dropX, top, bw, bh)
                        .build()
        );

        int takeX = left + 176 + gap;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Забрать все"), button -> DropAllHelper.takeCraftingItems())
                        .dimensions(takeX, top, bw, bh)
                        .build()
        );
    }
}
